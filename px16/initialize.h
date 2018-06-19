#include "uart1234.h"
#include "spi12.h"
#include "pins.h"
#include "interrupts.h"
#include "oscillator.h"

void sys_init()
{
	osc_init();		// Clock and Reference Clock	
	pin_init();		//Pin assignments and I/O
	isr_init();		//UART, SPI, Timer interrupts
	spi_ttn_init();	//SPI1 for TTN
	spi_ddac_init();//SPI2 for dual DAC
	uart_bt_init();
}
