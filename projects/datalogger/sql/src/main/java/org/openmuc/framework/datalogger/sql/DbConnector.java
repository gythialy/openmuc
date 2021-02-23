/*
 * Copyright 2011-2021 Fraunhofer ISE
 *
 * This file is part of OpenMUC.
 * For more information visit http://www.openmuc.org
 *
 * OpenMUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenMUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenMUC.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.openmuc.framework.datalogger.sql;

import static org.openmuc.framework.datalogger.sql.utils.SqlValues.MYSQL;
import static org.openmuc.framework.datalogger.sql.utils.SqlValues.POSTGRES;
import static org.openmuc.framework.datalogger.sql.utils.SqlValues.POSTGRESQL;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.Properties;

import javax.sql.DataSource;

import org.h2.tools.Server;
import org.openmuc.framework.datalogger.sql.utils.PropertyHandlerProvider;
import org.openmuc.framework.datalogger.sql.utils.Settings;
import org.openmuc.framework.lib.osgi.config.PropertyHandler;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jdbc.DataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbConnector {

    private final Logger logger = LoggerFactory.getLogger(DbConnector.class);
    private final PrintWriter out = new PrintWriter(System.out, true);
    private final String url;
    private Connection connection;
    private DataSource dataSource;
    private DataSourceFactory dataSourceFactory;
    private boolean timescaleActive;
    private java.sql.Driver driver;
    private Server server;

    public DbConnector() {
        PropertyHandler propertyHandler = PropertyHandlerProvider.getInstance().getPropertyHandler();
        url = propertyHandler.getString(Settings.URL);
        initConnector();
        getConnectionToDb();
    }

    private void initConnector() {
        BundleContext context = FrameworkUtil.getBundle(DbConnector.class).getBundleContext();
        ServiceReference<?> reference = context.getServiceReference(DataSourceFactory.class);
        dataSourceFactory = (DataSourceFactory) context.getService(reference);
    }

    /**
     * Starts up an H2 TCP server
     */
    public void startH2Server() {
        try {
            logger.info("Starting H2 Server");
            server = Server.createTcpServer("-webAllowOthers", "-tcpAllowOthers").start();
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Stops H2 TCP server
     */
    private void stopH2Server() {
        logger.info("Stopping H2 Server");
        server.stop();
    }

    public Statement createStatementWithConnection() throws SQLException {
        return connection.createStatement();
    }

    /**
     * Sets the proper dataSourceFactory, depending on the URL, using {@link #setDataSourceFactory()} and creates a
     * dataSource with it, creates a connection to the database and in case PostgreSQL is used it checks if timescale is
     * installed with {@link #checkIfTimescaleInstalled()} or needs to be updated with {@link #updateTimescale()}. If a
     * H2 database is corrupted it renames it so a new one is created using {@link #renameCorruptedDb()}.
     */
    private void getConnectionToDb() {
        try {
            logger.info("sql driver");
            if (connection == null || connection.isClosed()) {
                logger.debug("CONNECTING");
                Properties properties = setSqlProperties();
                logger.info(MessageFormat.format("URL is: {0}", url));

                setDataSourceFactory();

                dataSource = getDataSource(dataSourceFactory, properties);

                if (logger.isTraceEnabled()) {
                    dataSource.setLogWriter(out);
                }
                connection = dataSource.getConnection();
                if (url.contains(POSTGRES)) {
                    checkIfTimescaleInstalled();
                }
                if (url.contains(POSTGRES) && timescaleActive) {
                    updateTimescale();
                }
                logger.debug("CONNECTED");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(("SQLException: {0}" + e.getMessage()));
            logger.error(MessageFormat.format("SQLState:     {0}", e.getSQLState()));
            logger.error(MessageFormat.format("VendorError:  {0}", e.getErrorCode()));

            if (url.contains("h2") && e.getErrorCode() == 90030) {
                renameCorruptedDb();

            }
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    private synchronized DataSource getDataSource(DataSourceFactory dataSourceFactory, Properties properties)
            throws SQLException {
        if (dataSource == null) {
            dataSource = dataSourceFactory.createDataSource(properties);
        }
        return dataSource;
    }

    /**
     * returns a properties object with the attributes the datasource needs
     *
     * @return a properties object with the attributes the datasource needs
     */
    private Properties setSqlProperties() {
        PropertyHandler propertyHandler = PropertyHandlerProvider.getInstance().getPropertyHandler();
        Properties properties = new Properties();
        properties.setProperty("url", url);
        properties.setProperty("password", propertyHandler.getString(Settings.PASSWORD));
        properties.setProperty("user", propertyHandler.getString(Settings.USER));
        if (!url.contains("h2")) {
            if (url.contains(POSTGRESQL)) {
                properties.setProperty("ssl", propertyHandler.getString(Settings.SSL));
            }
            properties.setProperty("tcpKeepAlive", propertyHandler.getString(Settings.TCP_KEEP_ALIVE));
            properties.setProperty("socketTimeout", propertyHandler.getString(Settings.SOCKET_TIMEOUT));
        }

        return properties;
    }

    /**
     * Iterates over the bundles in the bundleContext and creates a new instance of the PostgreSQL or MySQL
     * dataSourceFactory. The MySQL JDBC driver needs the dataSourceFactory of OPS4J Pax JDBC Generic Driver Extender,
     * which has to be instantiated with the MySQL JDBC Driver class
     */
    private void setDataSourceFactory() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        BundleContext bundleContext = FrameworkUtil.getBundle(SqlLoggerService.class).getBundleContext();
        if (url.contains(POSTGRESQL)) {
            for (Bundle bundle : bundleContext.getBundles()) {
                if (bundle.getSymbolicName().equals("org.postgresql.jdbc")) {
                    dataSourceFactory = (DataSourceFactory) bundle.loadClass("org.postgresql.osgi.PGDataSourceFactory")
                            .newInstance();
                    // ToDo: make this running
                    // dataSourceFactory = new PGDataSourceFactory();
                }
            }
        }

        if (url.contains(MYSQL)) {
            for (Bundle bundle : bundleContext.getBundles()) {
                if (bundle.getSymbolicName().equals("com.mysql.cj")) {
                    // retrieve MySQL JDBC driver
                    driver = (java.sql.Driver) bundle.loadClass("com.mysql.cj.jdbc.Driver").newInstance();
                }
                if (bundle.getSymbolicName().equals("org.ops4j.pax.jdbc")) {
                    // get constructor and instantiate with MySQL driver
                    Constructor[] constructors = bundle.loadClass("org.ops4j.pax.jdbc.impl.DriverDataSourceFactory")
                            .getDeclaredConstructors();
                    Constructor constructor = constructors[0];
                    constructor.setAccessible(true);
                    try {
                        dataSourceFactory = (DataSourceFactory) constructor.newInstance(driver);
                    } catch (IllegalArgumentException e) {
                        logger.error(e.getMessage());
                    } catch (InvocationTargetException e) {
                        logger.error(e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Sets timescaleActive to true if timescale is installed
     */
    private void checkIfTimescaleInstalled() {
        StringBuilder sbExtensions = new StringBuilder("SELECT * FROM pg_extension;");

        try (ResultSet resultSet = connection.createStatement().executeQuery(sbExtensions.toString());) {
            while (resultSet.next()) {
                if (resultSet.getString("extname").contains("timescale")) {
                    timescaleActive = true;
                }
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Updates the PostgreSQL timescale extension by executing a SQL query as a console command
     */
    private void updateTimescale() {
        try {
            String line;
            String[] cmd = new String[3];
            int startPoint = url.lastIndexOf('/');
            String dbName = url.substring(startPoint + 1);
            if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                cmd[0] = "cmd.exe";
            }
            else {
                cmd[0] = "sh";
            }
            PropertyHandler propertyHandler = PropertyHandlerProvider.getInstance().getPropertyHandler();
            cmd[1] = "-c";
            cmd[2] = "PGPASSWORD=" + propertyHandler.getString(Settings.PSQL_PASS)
                    + " psql -c 'ALTER EXTENSION timescaledb UPDATE;'  -U postgres -h localhost -d " + dbName;
            Process process = Runtime.getRuntime().exec(cmd);

            BufferedReader stdOutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = stdOutReader.readLine()) != null) {
                logger.info(line);
            }

            BufferedReader stdErrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = stdErrReader.readLine()) != null) {
                logger.info(line);
            }

        } catch (Exception e) {
            logger.error(MessageFormat.format("Unable to execute shell command: {0}", e.getMessage()));
        }
    }

    /**
     * Renames the corrupted database to dbName"_corrupted_"timestamp, by building a with the classpath to it and
     * calling renameTo on it
     */
    private void renameCorruptedDb() {
        logger.error("Renaming corrupted Database so new one can be created");
        Timestamp renameTimestamp = new Timestamp(System.currentTimeMillis());
        String path = "";
        int endPoint = url.indexOf(';');
        if (url.contains("file")) {
            path = url.substring(19, endPoint);
        }
        if (url.contains("tcp")) {
            if (url.contains("~")) {
                path = System.getProperty("user.home") + url.substring(30, endPoint);
            }
            else {
                path = System.getProperty("user.home") + url.substring(28, endPoint);
            }
        }
        File sqlDb = new File(path + ".mv.db");
        File sqlDbOld = new File(path + "_corrupted_" + renameTimestamp + ".mv.db");
        boolean success = sqlDb.renameTo(sqlDbOld);
        if (success) {
            logger.info("Renaming successful, restarting sqlLogger");
            getConnectionToDb();
        }
        else {
            logger.info("Unable to rename corrupted Database");
        }
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                if (url.contains("h2") && url.contains("tcp")) {
                    stopH2Server();
                }
            } catch (SQLException e) {
                // ignore
            }
        }
    }
}
