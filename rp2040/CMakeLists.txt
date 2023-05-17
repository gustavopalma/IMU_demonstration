cmake_minimum_required(VERSION 3.13)

include(pico_sdk_import.cmake)

project(mpu6050_ble C CXX ASM)
set(CMAKE_C STANDARD 11)
set(CMAKE_CXX_STANDARD 17)
set(PICO_BOARD pico_w)
pico_sdk_init()

# Adds a CMakeLists.txt file from a subdirectory
add_subdirectory(vendors)

add_executable(mpu6050_ble
    main.c gatt_service.c
)
target_include_directories(mpu6050_ble PRIVATE ${CMAKE_CURRENT_LIST_DIR} )


target_link_libraries(mpu6050_ble pico_btstack_ble pico_btstack_cyw43 pico_cyw43_arch_none pico_cyw43_arch_lwip_threadsafe_background pico_stdlib haw_mpu6050)

target_include_directories(mpu6050_ble PRIVATE
    ${CMAKE_CURRENT_LIST_DIR} # For btstack config
    )

pico_btstack_make_gatt_header(mpu6050_ble PRIVATE "${CMAKE_CURRENT_LIST_DIR}/MPU_6050_GATT.gatt")

pico_enable_stdio_usb(mpu6050_ble 1)
pico_enable_stdio_uart(mpu6050_ble 0)

pico_add_extra_outputs(mpu6050_ble)

