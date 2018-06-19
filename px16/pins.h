void pin_init()
{
__builtin_write_OSCCONL(OSCCON & 0xbf); // unlock PPS

    RPOR7bits.RP15R = 0x03;   	// Set RP15 U1TX
    RPINR18bits.U1RXR = 0x0E;  	// Set RP14 U1RX

	//RPINR20bits.SDI1R = 0x09;	// Set RP9 SDI1
//	RPOR4bits.RP8R = 0x07;		// Set RP8 SDO1
//	RPOR3bits.RP7R = 0x08;		// Set RP7 SCK1OUT
//	RPOR1bits.RP3R = 0x09;		// Set RP3 SS1OUT

//	RPOR2bits.RP5R = 0x0A;		// Set RP13 SDO2
//	RPOR6bits.RP12R = 0x0B;		// Set RP12 SCK2OUT
//	RPOR5bits.RP11R = 0x0C;		// Set RP11 SS2OUT

	RPINR20bits.SDI1R = 0x08;	// Set RP8 SDI1
	RPOR4bits.RP9R = 0x07;		// Set RP9 SDO1
	//RPOR3bits.RP6R = 0x08;	// Set RP6 SCK1OUT RP6 PPS output does not work, errata
	RPOR2bits.RP5R = 0x08;		// Set RP5 SCK1OUT
	RPOR3bits.RP7R = 0x09;		// Set RP7 SS1OUT

	RPOR5bits.RP10R = 0x0A;		// Set RP10 SDO2
	RPOR5bits.RP11R = 0x0B;		// Set RP11 SCK2OUT
	RPOR6bits.RP12R = 0x0C;		// Set RP12 SS2OUT

__builtin_write_OSCCONL(OSCCON | 0x40); // lock PPS


	//PADCFG1 = 0xFF;			
	AD1CON1bits.ADON = 0;		// Disable ADC
	PMD1bits.ADC1MD = 1;		// Peripheral module disable for ADC
	ANSA = 0;					// Make analog pins digital 
	ANSB = 0;
	
	TRISB = 0x4103;				// Configure pins for I/O
	LATB = 0x0;
	LATBbits.LATB6 = 1;		// Hold LDAC high on RP6 (for DAC)			



}
