void osc_init()	
{
	CLKDIV = 0;
	CLKDIVbits.PLLEN = 1;	

	OSCTUNbits.STEN = 1;
	OSCTUNbits.TUN = 0b100000;
	
	REFOCONLbits.ROOUT = 0;	// Disable REFO pin
	REFOCONLbits.ROEN = 0;	// Disable REFO module
	REFOCONLbits.ROSEL = 0x3; // Use FRC 8MHz clock
	//while(REFOCONLbits.ROACTIVE);
	//REFOCONLbits.ROSWEN = 0;
	REFOCONH = 0x0001;		// Divide by 2xREFOCONH = 4
	REFOTRIML = 0x0000;		// No inclusion of fractional divisor
	REFOCONLbits.ROSWEN = 1;	// Switch the clock
	//while(REFOCONLbits.ROSWEN);
	REFOCONLbits.ROEN = 1;	// Enable REFO module
	REFOCONLbits.ROOUT = 1;	// Enable REFO pin
}
