#ifndef GATT_SERVER_H
#define GATT_SERVER_H

extern int le_notification_enabled;
extern hci_con_handle_t con_handle;
extern float acceleration[3], gyro[3], current_temp;
extern uint8_t const profile_data[];


void packet_handler(uint8_t packet_type, uint16_t channel, uint8_t *packet, uint16_t size);
uint16_t att_read_callback(hci_con_handle_t connection_handle, uint16_t att_handle, uint16_t offset, uint8_t * buffer, uint16_t buffer_size);
int att_write_callback(hci_con_handle_t connection_handle, uint16_t att_handle, uint16_t transaction_mode, uint16_t offset, uint8_t *buffer, uint16_t buffer_size);
void poll_imu_mpu_6050(void);

#endif