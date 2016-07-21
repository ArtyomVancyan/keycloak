/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.connections.jpa;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.hibernate.ejb.AvailableSettings;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.jpa.updater.JpaUpdaterProvider;
import org.keycloak.connections.jpa.util.JpaUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.dblock.DBLockProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.models.dblock.DBLockManager;
import org.keycloak.timer.TimerProvider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultJpaConnectionProviderFactory implements JpaConnectionProviderFactory, ServerInfoAwareProviderFactory {

    private static final Logger logger = Logger.getLogger(DefaultJpaConnectionProviderFactory.class);

    private volatile EntityManagerFactory emf;

    private Config.Scope config;
    
    private Map<String,String> operationalInfo;

    @Override
    public JpaConnectionProvider create(KeycloakSession session) {
        lazyInit(session);

        EntityManager em = emf.createEntityManager();
        em = PersistenceExceptionConverter.create(em);
        session.getTransaction().enlist(new JpaKeycloakTransaction(em));
        return new DefaultJpaConnectionProvider(em);
    }

    @Override
    public void close() {
        if (emf != null) {
            emf.close();
        }
    }

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    private void lazyInit(KeycloakSession session) {
        if (emf == null) {
            synchronized (this) {
                if (emf == null) {
                    logger.debug("Initializing JPA connections");

                    Map<String, Object> properties = new HashMap<String, Object>();

                    String unitName = "keycloak-default";

                    String dataSource = config.get("dataSource");
                    if (dataSource != null) {
                        if (config.getBoolean("jta", false)) {
                            properties.put(AvailableSettings.JTA_DATASOURCE, dataSource);
                        } else {
                            properties.put(AvailableSettings.NON_JTA_DATASOURCE, dataSource);
                        }
                    } else {
                        properties.put(AvailableSettings.JDBC_URL, config.get("url"));
                        properties.put(AvailableSettings.JDBC_DRIVER, config.get("driver"));

                        String user = config.get("user");
                        if (user != null) {
                            properties.put(AvailableSettings.JDBC_USER, user);
                        }
                        String password = config.get("password");
                        if (password != null) {
                            properties.put(AvailableSettings.JDBC_PASSWORD, password);
                        }
                    }

                    String schema = getSchema();
                    if (schema != null) {
                        properties.put(JpaUtils.HIBERNATE_DEFAULT_SCHEMA, schema);
                    }


                    String databaseSchema;
                    String databaseSchemaConf = config.get("databaseSchema");
                    if (databaseSchemaConf == null) {
                        throw new RuntimeException("Property 'databaseSchema' needs to be specified in the configuration");
                    }
                    
                    if (databaseSchemaConf.equals("development-update")) {
                        properties.put("hibernate.hbm2ddl.auto", "update");
                        databaseSchema = null;
                    } else if (databaseSchemaConf.equals("development-validate")) {
                        properties.put("hibernate.hbm2ddl.auto", "validate");
                        databaseSchema = null;
                    } else {
                        databaseSchema = databaseSchemaConf;
                    }

                    properties.put("hibernate.show_sql", config.getBoolean("showSql", false));
                    properties.put("hibernate.format_sql", config.getBoolean("formatSql", true));

                    Connection connection = getConnection();
                    try{ 
	                    prepareOperationalInfo(connection);

                        String driverDialect = detectDialect(connection);
                        if (driverDialect != null) {
                            properties.put("hibernate.dialect", driverDialect);
                        }
	                    
	                    if (databaseSchema != null) {
	                        logger.trace("Updating database");
	
	                        JpaUpdaterProvider updater = session.getProvider(JpaUpdaterProvider.class);
	                        if (updater == null) {
	                            throw new RuntimeException("Can't update database: JPA updater provider not found");
	                        }

                            // Check if having DBLock before trying to initialize hibernate
                            DBLockProvider dbLock = new DBLockManager(session).getDBLock();
                            if (dbLock.hasLock()) {
                                updateOrValidateDB(databaseSchema, connection, updater, schema);
                            } else {
                                logger.trace("Don't have DBLock retrieved before upgrade. Needs to acquire lock first in separate transaction");

                                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {

                                    @Override
                                    public void run(KeycloakSession lockSession) {
                                        DBLockManager dbLockManager = new DBLockManager(lockSession);
                                        DBLockProvider dbLock2 = dbLockManager.getDBLock();
                                        dbLock2.waitForLock();
                                        try {
                                            updateOrValidateDB(databaseSchema, connection, updater, schema);
                                        } finally {
                                            dbLock2.releaseLock();
                                        }
                                    }

                                });
                            }
	                    }

                        int globalStatsInterval = config.getInt("globalStatsInterval", -1);
                        if (globalStatsInterval != -1) {
                            properties.put("hibernate.generate_statistics", true);
                        }

	                    logger.trace("Creating EntityManagerFactory");
	                    emf = JpaUtils.createEntityManagerFactory(session, unitName, properties, getClass().getClassLoader());
	                    logger.trace("EntityManagerFactory created");

                        if (globalStatsInterval != -1) {
                            startGlobalStats(session, globalStatsInterval);
                        }

                    } catch (Exception e) {
                        // Safe rollback
                        if (connection != null) {
                            try {
                                connection.rollback();
                            } catch (SQLException e2) {
                                logger.warn("Can't rollback connection", e2);
                            }
                        }

                        throw e;
                    } finally {
	                    // Close after creating EntityManagerFactory to prevent in-mem databases from closing
	                    if (connection != null) {
	                        try {
	                            connection.close();
	                        } catch (SQLException e) {
	                            logger.warn("Can't close connection", e);
	                        }
	                    }
                    }
                }
            }
        }
    }

    protected void prepareOperationalInfo(Connection connection) {
  		try {
  			operationalInfo = new LinkedHashMap<>();
  			DatabaseMetaData md = connection.getMetaData();
  			operationalInfo.put("databaseUrl",md.getURL());
  			operationalInfo.put("databaseUser", md.getUserName());
  			operationalInfo.put("databaseProduct", md.getDatabaseProductName() + " " + md.getDatabaseProductVersion());
  			operationalInfo.put("databaseDriver", md.getDriverName() + " " + md.getDriverVersion());

            logger.debugf("Database info: %s", operationalInfo.toString());
  		} catch (SQLException e) {
  			logger.warn("Unable to prepare operational info due database exception: " + e.getMessage());
  		}
  	}


    protected String detectDialect(Connection connection) {
        String driverDialect = config.get("driverDialect");
        if (driverDialect != null && driverDialect.length() > 0) {
            return driverDialect;
        } else {
            try {
                String dbProductName = connection.getMetaData().getDatabaseProductName();
                String dbProductVersion = connection.getMetaData().getDatabaseProductVersion();

                // For MSSQL2014, we may need to fix the autodetected dialect by hibernate
                if (dbProductName.equals("Microsoft SQL Server")) {
                    String topVersionStr = dbProductVersion.split("\\.")[0];
                    boolean shouldSet2012Dialect = true;
                    try {
                        int topVersion = Integer.parseInt(topVersionStr);
                        if (topVersion < 12) {
                            shouldSet2012Dialect = false;
                        }
                    } catch (NumberFormatException nfe) {
                    }
                    if (shouldSet2012Dialect) {
                        String sql2012Dialect = "org.hibernate.dialect.SQLServer2012Dialect";
                        logger.debugf("Manually override hibernate dialect to %s", sql2012Dialect);
                        return sql2012Dialect;
                    }
                }
            } catch (SQLException e) {
                logger.warnf("Unable to detect hibernate dialect due database exception : %s", e.getMessage());
            }

            return null;
        }
    }

    protected void startGlobalStats(KeycloakSession session, int globalStatsIntervalSecs) {
        logger.debugf("Started Hibernate statistics with the interval %s seconds", globalStatsIntervalSecs);
        TimerProvider timer = session.getProvider(TimerProvider.class);
        timer.scheduleTask(new HibernateStatsReporter(emf), globalStatsIntervalSecs * 1000, "ReportHibernateGlobalStats");
    }


    // Needs to be called with acquired DBLock
    protected void updateOrValidateDB(String databaseSchema, Connection connection, JpaUpdaterProvider updater, String schema) {
        if (databaseSchema.equals("update")) {
            updater.update(connection, schema);
            logger.trace("Database update completed");
        } else if (databaseSchema.equals("validate")) {
            updater.validate(connection, schema);
            logger.trace("Database validation completed");
        } else {
            throw new RuntimeException("Invalid value for databaseSchema: " + databaseSchema);
        }
    }


    @Override
    public Connection getConnection() {
        try {
            String dataSourceLookup = config.get("dataSource");
            if (dataSourceLookup != null) {
                DataSource dataSource = (DataSource) new InitialContext().lookup(dataSourceLookup);
                return dataSource.getConnection();
            } else {
                Class.forName(config.get("driver"));
                return DriverManager.getConnection(config.get("url"), config.get("user"), config.get("password"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    @Override
    public String getSchema() {
        return config.get("schema");
    }
    
    @Override
  	public Map<String,String> getOperationalInfo() {
  		return operationalInfo;
  	}

}
