#include <stdio.h>
#include "btstack.h"

#include "MPU_6050_GATT.h"
#include "gatt_service.h"
#include "haw/MPU6050.h"

#define APP_AD_FLAGS 0x06
static uint8_t adv_data[] = {
    // Flags general discoverable
    0x02, BLUETOOTH_DATA_TYPE_FLAGS, APP_AD_FLAGS,
    // Name
    0x0B, BLUETOOTH_DATA_TYPE_COMPLETE_LOCAL_NAME, 'P', 'i', 'c', 'o', ' ', 'W', ' ', 'I', 'M', 'U',
    0x03, BLUETOOTH_DATA_TYPE_COMPLETE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS, 0x1a, 0x18,
};
static const uint8_t adv_data_len = sizeof(adv_data);

int le_notification_enabled;
hci_con_handle_t con_handle;
float acceleration[3] = {0,0,0}, gyro[3] = {0,0,0},  current_temp = 0;
mpu6050_t mpu6050;
bool initialized = false;

void packet_handler(uint8_t packet_type, uint16_t channel, uint8_t *packet, uint16_t size) {
    UNUSED(size);
    UNUSED(channel);
    bd_addr_t local_addr;
    if (packet_type != HCI_EVENT_PACKET) return;

    uint8_t event_type = hci_event_packet_get_type(packet);
        switch(event_type){
        case BTSTACK_EVENT_STATE:
            if (btstack_event_state_get_state(packet) != HCI_STATE_WORKING) return;
            gap_local_bd_addr(local_addr);
            printf("BTstack up and running on %s.\n", bd_addr_to_str(local_addr));

            // setup advertisements
            uint16_t adv_int_min = 800;
            uint16_t adv_int_max = 800;
            uint8_t adv_type = 0;
            bd_addr_t null_addr;
            memset(null_addr, 0, 6);
            gap_advertisements_set_params(adv_int_min, adv_int_max, adv_type, 0, null_addr, 0x07, 0x00);
            assert(adv_data_len <= 31); // ble limitation
            gap_advertisements_set_data(adv_data_len, (uint8_t*) adv_data);
            gap_advertisements_enable(1);

            poll_imu_mpu_6050();

            break;
        case HCI_EVENT_DISCONNECTION_COMPLETE:
            le_notification_enabled = 0;
            break;
        case ATT_EVENT_CAN_SEND_NOW:
            att_server_notify(con_handle, ATT_CHARACTERISTIC_ORG_BLUETOOTH_CHARACTERISTIC_TEMPERATURE_01_VALUE_HANDLE, (uint8_t*)&current_temp, sizeof(current_temp));
            att_server_notify(con_handle, ATT_CHARACTERISTIC_e7890e92_ed43_11ed_a05b_0242ac120003_01_VALUE_HANDLE, (uint8_t*)&acceleration[0], sizeof(acceleration[0]));
            att_server_notify(con_handle, ATT_CHARACTERISTIC_9b788ed0_f04d_11ed_a05b_0242ac120003_01_VALUE_HANDLE, (uint8_t*)&acceleration[1], sizeof(acceleration[1]));
            att_server_notify(con_handle, ATT_CHARACTERISTIC_9b789218_f04d_11ed_a05b_0242ac120003_01_VALUE_HANDLE, (uint8_t*)&acceleration[2], sizeof(acceleration[2]));
            att_server_notify(con_handle, ATT_CHARACTERISTIC_9b789470_f04d_11ed_a05b_0242ac120003_01_VALUE_HANDLE, (uint8_t*)&gyro[0], sizeof(gyro[0]));
            att_server_notify(con_handle, ATT_CHARACTERISTIC_9b789632_f04d_11ed_a05b_0242ac120003_01_VALUE_HANDLE, (uint8_t*)&gyro[1], sizeof(gyro[1]));
            att_server_notify(con_handle, ATT_CHARACTERISTIC_9b7897d6_f04d_11ed_a05b_0242ac120003_01_VALUE_HANDLE, (uint8_t*)&gyro[2], sizeof(gyro[2]));
            break;
        default:
            break;
    }
}

uint16_t att_read_callback(hci_con_handle_t connection_handle, uint16_t att_handle, uint16_t offset, uint8_t * buffer, uint16_t buffer_size) {
    UNUSED(connection_handle);   
    switch (att_handle) {
        case ATT_CHARACTERISTIC_e7890e92_ed43_11ed_a05b_0242ac120003_01_VALUE_HANDLE:
            return att_read_callback_handle_blob((const uint8_t *)&acceleration[0], sizeof(acceleration[0]), offset, buffer, buffer_size);
        break;

        case ATT_CHARACTERISTIC_9b788ed0_f04d_11ed_a05b_0242ac120003_01_VALUE_HANDLE:
            return att_read_callback_handle_blob((const uint8_t *)&acceleration[1], sizeof(acceleration[1]), offset, buffer, buffer_size);
        break;

        case ATT_CHARACTERISTIC_9b789218_f04d_11ed_a05b_0242ac120003_01_VALUE_HANDLE:
            return att_read_callback_handle_blob((const uint8_t *)&acceleration[2], sizeof(acceleration[2]), offset, buffer, buffer_size);
        break;

        case ATT_CHARACTERISTIC_9b789470_f04d_11ed_a05b_0242ac120003_01_VALUE_HANDLE:
            return att_read_callback_handle_blob((const uint8_t *)&gyro[0], sizeof(gyro[0]), offset, buffer, buffer_size);
        break;

        case ATT_CHARACTERISTIC_9b789632_f04d_11ed_a05b_0242ac120003_01_VALUE_HANDLE:
            return att_read_callback_handle_blob((const uint8_t *)&gyro[1], sizeof(gyro[1]), offset, buffer, buffer_size);
        break;

        case ATT_CHARACTERISTIC_9b7897d6_f04d_11ed_a05b_0242ac120003_01_VALUE_HANDLE:
            return att_read_callback_handle_blob((const uint8_t *)&gyro[2], sizeof(gyro[2]), offset, buffer, buffer_size);
        break;

        case ATT_CHARACTERISTIC_ORG_BLUETOOTH_CHARACTERISTIC_TEMPERATURE_01_VALUE_HANDLE:
            return att_read_callback_handle_blob((const uint8_t *)&current_temp, sizeof(current_temp), offset, buffer, buffer_size);
        break;
    }
    return 0;
}

int att_write_callback(hci_con_handle_t connection_handle, uint16_t att_handle, uint16_t transaction_mode, uint16_t offset, uint8_t *buffer, uint16_t buffer_size) {
    UNUSED(transaction_mode);
    UNUSED(offset);
    UNUSED(buffer_size);
    switch (att_handle) {
        case ATT_CHARACTERISTIC_e7890e92_ed43_11ed_a05b_0242ac120003_01_CLIENT_CONFIGURATION_HANDLE:
            le_notification_enabled = little_endian_read_16(buffer, 0) == GATT_CLIENT_CHARACTERISTICS_CONFIGURATION_NOTIFICATION;
        break;

        case ATT_CHARACTERISTIC_9b788ed0_f04d_11ed_a05b_0242ac120003_01_CLIENT_CONFIGURATION_HANDLE:
            le_notification_enabled = little_endian_read_16(buffer, 0) == GATT_CLIENT_CHARACTERISTICS_CONFIGURATION_NOTIFICATION;
        break;

        case ATT_CHARACTERISTIC_9b789218_f04d_11ed_a05b_0242ac120003_01_CLIENT_CONFIGURATION_HANDLE:
            le_notification_enabled = little_endian_read_16(buffer, 0) == GATT_CLIENT_CHARACTERISTICS_CONFIGURATION_NOTIFICATION;
        break;

        case ATT_CHARACTERISTIC_9b789470_f04d_11ed_a05b_0242ac120003_01_CLIENT_CONFIGURATION_HANDLE:
            le_notification_enabled = little_endian_read_16(buffer, 0) == GATT_CLIENT_CHARACTERISTICS_CONFIGURATION_NOTIFICATION;
        break;

        case ATT_CHARACTERISTIC_9b789632_f04d_11ed_a05b_0242ac120003_01_CLIENT_CONFIGURATION_HANDLE:
            le_notification_enabled = little_endian_read_16(buffer, 0) == GATT_CLIENT_CHARACTERISTICS_CONFIGURATION_NOTIFICATION;
        break;

        case ATT_CHARACTERISTIC_9b7897d6_f04d_11ed_a05b_0242ac120003_01_CLIENT_CONFIGURATION_HANDLE:
            le_notification_enabled = little_endian_read_16(buffer, 0) == GATT_CLIENT_CHARACTERISTICS_CONFIGURATION_NOTIFICATION;
        break;

        case ATT_CHARACTERISTIC_ORG_BLUETOOTH_CHARACTERISTIC_TEMPERATURE_01_CLIENT_CONFIGURATION_HANDLE:
            le_notification_enabled = little_endian_read_16(buffer, 0) == GATT_CLIENT_CHARACTERISTICS_CONFIGURATION_NOTIFICATION;
        break;
    }
    
    if (le_notification_enabled) {
        con_handle = connection_handle;
        att_server_request_can_send_now_event(con_handle);
    }
    return 0;
}

void poll_imu_mpu_6050(void) {
    if  (! initialized) {
        mpu6050 = mpu6050_init(i2c_default, MPU6050_ADDRESS_A0_GND);

        // Check if the MPU6050 can initialize
        if (mpu6050_begin(&mpu6050))
        {
            // Set scale of gyroscope
            mpu6050_set_scale(&mpu6050, MPU6050_SCALE_2000DPS);
            // Set range of accelerometer
            mpu6050_set_range(&mpu6050, MPU6050_RANGE_16G);

            // Enable temperature, gyroscope and accelerometer readings
            mpu6050_set_temperature_measuring(&mpu6050, true);
            mpu6050_set_gyroscope_measuring(&mpu6050, true);
            mpu6050_set_accelerometer_measuring(&mpu6050, true);

            // Enable free fall, motion and zero motion interrupt flags
            mpu6050_set_int_free_fall(&mpu6050, false);
            mpu6050_set_int_motion(&mpu6050, false);
            mpu6050_set_int_zero_motion(&mpu6050, false);

            // Set motion detection threshold and duration
            mpu6050_set_motion_detection_threshold(&mpu6050, 2);
            mpu6050_set_motion_detection_duration(&mpu6050, 5);

            // Set zero motion detection threshold and duration
            mpu6050_set_zero_motion_detection_threshold(&mpu6050, 4);
            mpu6050_set_zero_motion_detection_duration(&mpu6050, 2);
            initialized = true;
        }
        else
        {
            while (1)
            {
                // Endless loop
                printf("Error! MPU6050 could not be initialized. Make sure you've entered the correct address. And double check your connections.");
                sleep_ms(500);
            }
        }
    }
       
    // Fetch all data from the sensor | I2C is only used here
    mpu6050_event(&mpu6050);

    // Pointers to float vectors with all the results
    mpu6050_vectorf_t *accel = mpu6050_get_accelerometer(&mpu6050);
    mpu6050_vectorf_t *gyr = mpu6050_get_gyroscope(&mpu6050);

    // Rough temperatures as float -- Keep in mind, this is not a temperature sensor!!!
    current_temp = mpu6050_get_temperature_c(&mpu6050);

    // Print all the measurements
    printf("Accelerometer: %f, %f, %f - Gyroscope: %f, %f, %f - Temperature: %f°C\n", accel->x, accel->y, accel->z, gyr->x, gyr->y, gyr->z, current_temp);
    acceleration[0] =  accel->x;
    acceleration[1] =  accel->y;
    acceleration[2] = accel->z;
    gyro[0] = gyr->x;
    gyro[1] = gyr->y;
    gyro[2] = gyr->z;
 }