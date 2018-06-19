void isr_init()
{	
	// Note that all interrupts are reset to priority 4
	INTCON1bits.NSTDIS = 1;	// Disable nested interrupts

	// Disable SPI1 interrupts
	IEC0bits.SPI1TXIE = 0;	// Tx
	IEC0bits.SPI1IE = 0;	// General
	IEC3bits.SPI1RXIE = 0;	// Rx

	// Clear SPI1 interrupt flags
	IFS0bits.SPI1TXIF = 0;
	IFS0bits.SPI1IF = 0;
	IFS3bits.SPI1RXIF = 0;

	// SPI1 ISR Priority
	//IPC2bits.SPI1TXIP = 0x05;
	//IPC2bits.SPI1IP = 0x05;
	//IPC14bits.SPI1RXIP = 0x05;

	// Disable SPI2 interrupts
	IEC2bits.SPI2TXIE = 0;	// Tx
	IEC2bits.SPI2IE = 0;	// General
	IEC3bits.SPI2RXIE = 0;	// Rx

	// Clear SPI2 interrupt flags
	IFS2bits.SPI2TXIF = 0;
	IFS2bits.SPI2IF = 0;
	IFS3bits.SPI2RXIF = 0;

	// SPI2 ISR Priority
	//IPC8bits.SPI2TXIP = 0x05;
	//IPC8bits.SPI2IP = 0x05;
	//IPC14bits.SPI2RXIP = 0x05;

	// Enable SPI2 interrupts
	//IEC3bits.SPI2RXIE = 1;	// Rx

	IEC0bits.U1TXIE = 0;	// Disable UART1 Transmit Interrupts
	IFS0bits.U1TXIF = 0;	// Clear the U1 Transmit Interrupt Flag
	
	IEC0bits.U1RXIE = 0;	// Disable UART1 Recieve Interrupts
	//IPC2bits.U1RXIP = 0x05;// Set Interrupt Priority Level 5
	IFS0bits.U1RXIF = 0;	// Clear the Recieve Interrupt Flag	
	IEC0bits.U1RXIE = 1;	// Enable Recieve Interrupts
	
}

void __attribute__((__interrupt__, auto_psv)) _U1RXInterrupt(void)
{   
	//LATBbits.LATB2 = 1;		// Turn on LED
	if (U1STAbits.OERR == 1) // Clear RX overrun flag
	{
		U1STAbits.OERR = 0;
	} 
	else 
	{
   		while(U1STAbits.URXDA)
		{
			if(ttn_cmd == 0){
				btrx = U1RXREG;
				switch(btrx){
					case 'Q': ttn_cmd = 1;
					break;
					case 'R': ttn_cmd = 2;
					break;
					case 'T': ttn_cmd = 3;
					break;
					case 'U': ttn_cmd = 4;
					break;
					case 'S': count = 0;
							  U1TXREG = 's';
							  while(U1STAbits.TRMT==0);
							  pxe_ffy(end);
							  ttn_cmd = 9;
					break;
					case 'V': count = 0;
							  ttn_cmd = 10;
					break;
					case 'W': ttn_cmd = 7;						
					break;
					case 'C': ttn_cmd = 8;
					break;
					}
			}
			else if(ttn_cmd == 6){
				btrx = U1RXREG;		
				if(btrx == 'W'){
					ttn_cmd = 7;
				}	
				else if(btrx == 'C'){
					ttn_cmd = 8;
				}	
			}
			else if(ttn_cmd == 7){
				btrx = U1RXREG;			
				if(btrx == 'X'){
					ttn_cmd = 0;
				}	
			}
			else if(ttn_cmd == 9){
				btrx = U1RXREG;
				if(btrx == 0xFF){
					ttn_cmd = 0;
				}
				else{
					tempfootprint[count] = btrx;
					count++;
					if(count == 988){
						count = 0;
						ttn_cmd = 5;
					}
				}
			}
			else if(ttn_cmd == 10){
				btrx = U1RXREG;
				tau63[count] = btrx;
				count++;
				if(count == 2){
					count = 0;
					ttn_cmd = 6;
				}
			}
  		}  	
	}
//	LATBbits.LATB2 = 0;		// Turn off LED
	IFS0bits.U1RXIF = 0; 
}
