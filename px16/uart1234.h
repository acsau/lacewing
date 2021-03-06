//#define u1baudrate       115200ul  			//Baud rate in bits-per-second
#define u1baudrate       38400ul
 
void uart_bt_init()
{
	//CloseUART1();
	//U1MODE = 0x0000; 		// Configure UART; 8-bits; no parity; 1 Stop bit; BRGH = 0;
	U1MODE = 0x0008; 		// Configure UART; 8-bits; no parity; 1 Stop bit; BRGH = 1;

	U1STA  = 0x0000; 		// Reset UART status register, define Rx interrupt condition, clear RSR FIFO	

	//unsigned int BRG = ((FCY/BAUDRATE)/16)-1; // Baud Rate register (BRGH = 0)
	unsigned int u1baud = ((FCY/u1baudrate)/4)-1; // Baud Rate register (BRGH = 1)
	U1BRG  = u1baud;
		
	U1MODEbits.UARTEN = 1;	// Enable UART1
	U1STAbits.UTXEN = 1;	// Enable UART1 Tx

	//OpenUART1(U1MODE, U1STA, U1BRG);
}

//Function to output data on UART1
void uart1_tx(char * data)
{
	int len, count;
	len = strlen(data);
	for (count = 0; count < len; count++) 
	{
		U1TXREG = (data[count]);		
		while(U1STAbits.TRMT==0);
	}
}

//void uart1_tx(char *data)
//{
//	int len, count;
//	len = strlen(data);
//	for (count = 0; count < len; count++) 
//	{
//		U1TXREG = (data[count]);		
//		while(U1STAbits.TRMT==0);
//	}
//}
