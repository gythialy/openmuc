/*
 * This file is part of OpenMUC.
 * For more information visit http://www.openmuc.org
 *
 * You are free to use code of this sample file in any
 * way you like and without any restrictions.
 *
 */
package org.openmuc.framework.app.simpledemo;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.dataaccess.RecordListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = {})
public final class SimpleDemoApp extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(SimpleDemoApp.class);
    private static final DecimalFormatSymbols DFS = DecimalFormatSymbols.getInstance(Locale.US);
    private static final DecimalFormat DF = new DecimalFormat("#0.000", DFS);

    // ChannelIDs, see conf/channel.xml
    private static final String ID_POWER_ELECTIC_VEHICLE = "power_electric_vehicle";
    private static final String ID_POWER_GRID = "power_grid";
    private static final String ID_STATUS_ELECTRIC_VEHICLE = "status_electric_vehicle";
    private static final String ID_ENERGY_EXPORTED = "energy_exported";
    private static final String ID_ENERGY_IMPORTED = "energy_imported";

    private static final double STANDBY_POWER_CHARGING_STATION = 0.020;

    // for conversion from power (kW) to energy (kWh)
    private static final double SECONDS_PER_HOUR = 3600.0;
    private static final double SECONDS_PER_INTERVAL = 5.0;
    private static final double HOUR_BASED_INTERVAL_TIME = SECONDS_PER_INTERVAL / SECONDS_PER_HOUR;

    private volatile boolean deactivatedSignal;

    // With the dataAccessService you can access to your measured and control data of your devices.
    @Reference
    private DataAccessService dataAccessService;

    // Channel for accessing data of a channel.
    private Channel chPowerElecticVehicle;
    private Channel chPowerGrid;
    private Channel chEvStatus;
    private Channel chEnergyExported;
    private Channel chEnergyImported;

    private double energyExportedKWh = 0;
    private double energyImportedKWh = 0;

    int printCounter; // for slowing down the output of the console

    /**
     * Every app needs one activate method. Is is called at begin. Here you can configure all you need at start of your
     * app. The Activate method can block the start of your OpenMUC, f.e. if you use Thread.sleep().
     * 
     * @param context
     */
    @Activate
    private void activate() {
        logger.info("Activating Demo App");
        setName("OpenMUC Simple Demo App");
        start();
    }

    /**
     * Every app needs one deactivate method. It handles the shutdown of your app e.g. closing open streams.
     * 
     * @param context
     */
    @Deactivate
    private void deactivate() {
        logger.info("Deactivating Demo App");
        deactivatedSignal = true;

        interrupt();
        try {
            this.join();
        } catch (InterruptedException e) {
        }
    }

    /**
     * application logic
     */
    @Override
    public void run() {

        logger.info("Demo App started running...");

        if (deactivatedSignal) {
            logger.info("DemoApp thread interrupted: will stop");
            return;
        }

        initializeChannels();
        applyListener();

        while (!deactivatedSignal) {
            updateEvStatusChannel();
            sleepMs(5000);
        }

    }

    /**
     * Initialize channel objects
     */
    private void initializeChannels() {
        chPowerElecticVehicle = dataAccessService.getChannel(ID_POWER_ELECTIC_VEHICLE);
        chPowerGrid = dataAccessService.getChannel(ID_POWER_GRID);
        chEvStatus = dataAccessService.getChannel(ID_STATUS_ELECTRIC_VEHICLE);
        chEnergyExported = dataAccessService.getChannel(ID_ENERGY_EXPORTED);
        chEnergyImported = dataAccessService.getChannel(ID_ENERGY_IMPORTED);
    }

    /**
     * Apply a RecordListener to get notified if a new value is available for a channel
     */
    private void applyListener() {
        chPowerGrid.addListener(new RecordListener() {
            @Override
            public void newRecord(Record record) {
                if (record.getValue() != null) {
                    updateEnergyChannels(record);
                }
            }
        });
    }

    /**
     * Calculate energy imported and exported from current grid power. (Demonstrates how to access the latest record of
     * a channel and how to set it.)
     * 
     * @param gridPowerRecord
     */
    private void updateEnergyChannels(Record gridPowerRecord) {

        double gridPower = gridPowerRecord.getValue().asDouble();
        logger.info("home1: current grid power = " + gridPower + " kW");

        double energyOfInterval = Math.abs(gridPower) * HOUR_BASED_INTERVAL_TIME;
        long now = System.currentTimeMillis();

        if (gridPower >= 0) {
            energyImportedKWh += energyOfInterval;
        }
        else {
            energyExportedKWh += energyOfInterval;
        }

        DoubleValue exportDouble = new DoubleValue(Double.parseDouble(DF.format(energyExportedKWh)));
        Record exportRecord = new Record(exportDouble, now, Flag.VALID);
        chEnergyExported.setLatestRecord(exportRecord);

        DoubleValue importDouble = new DoubleValue(Double.parseDouble(DF.format(energyImportedKWh)));
        Record importRecord = new Record(importDouble, now, Flag.VALID);
        chEnergyImported.setLatestRecord(importRecord);

    }

    /**
     * Checks if the electric vehicle is charging (Demonstrates how to access a value from a channel and how to set a
     * value/record)
     */
    private void updateEvStatusChannel() {
        double evPower;
        String status = "idle";

        // get current value of the electric vehicle power channel
        Record lastRecord = chPowerElecticVehicle.getLatestRecord();
        if (lastRecord != null) {
            Value value = lastRecord.getValue();
            if (value != null) {
                evPower = chPowerElecticVehicle.getLatestRecord().getValue().asDouble();
                if (evPower > STANDBY_POWER_CHARGING_STATION) {
                    status = "charging";
                }
                // set value for virtual channel
                Record newRecord = new Record(new StringValue(status), System.currentTimeMillis(), Flag.VALID);
                chEvStatus.setLatestRecord(newRecord);
            }
        }
    }

    private void sleepMs(long timeInMs) {
        try {
            Thread.sleep(timeInMs);
        } catch (InterruptedException e) {
        }
    }
}
