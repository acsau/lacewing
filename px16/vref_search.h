
// Function to get ctr_hi and ctr_low per frame
void ctr(int X, int MAX, int ctr_hi, int ctr_low){
	int vmax, vmin;
	for(col=0; col<col_max; col++){
		for(row=0; row<row_max; row++){						
			// Change pixel
			ttn_rowcol(row, col);			
			// Obtain V_min and V_max
			ttn_pxvo(0);
			vmin = read;
			ttn_pxvo(1023);
			vmax = read;			
			// Count ctr_hi and ctr_low
			if (vmin < X && vmax < X) {ctr_hi++;}
			if (vmin > MAX-X && vmax > MAX-X){ctr_low++;}
		}
	}	
}

uint16_t vref_search(){
// Initialize variables	
	uint16_t vref = 512;		// Middle of the modified DAC output range
	uint16_t step = 512;		// Half the DAC output range
	int ctr_hi = 0;
	int ctr_low = 0;
	int X = 150;
	int Y = 800;
	int Z = 100;
	int MAX = 2350;
	int tol = Y + Z;
	int done = 0;
	
	// Pre-pended stage...some procedure that checks for dead pixels, returning integer Y
	
	// Binary search algorithm	
	while(done == 0 && step >= 4){
		ctr_hi = 0;
		ctr_low = 0;
		//SPI2BUFL = spi_dac_reg(0, 1, 1, 1, vref);
		//__delay_us(10);
		//LATBbits.LATB6 = 0;		// Hold LDAC low
		//__delay_us(5);			
		//LATBbits.LATB6 = 1;		// Hold LDAC high
		
		ctr(X, MAX, ctr_hi, ctr_low);	// Get ctr_hi and ctr_low at this vref
		
		// Adjust vref
		if (abs(ctr_hi - ctr_low) < tol) {done = 1;}
		else if (ctr_hi > ctr_low) {vref -= step;}
		else {vref += step;}
		
		step = step/2;
	}

	return vref;
}



