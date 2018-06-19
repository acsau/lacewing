#include "p24FJ128GA202.h"
#include "config.h"
#include <xc.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

#define FCY 32000000ul       //Instruction Cycle Frequency (FRCPLL: Fcy = Fosc/2 = 64Mhz/2)

#include <libpic30.h>
#include "variables.h"
#include "initialize.h"

//MAIN
int main()
{	
	sys_init();
// START WHILE LOOP
	while(1){
		switch(ttn_cmd){
			case 1: // INITALIZE TTN
				LATBbits.LATB2 = 1;		// Turn on LED

				// Reset TTN;
				// Reset duration per px = 5 for 8MHz; 
				// PWM max width = 90 to cap digital value
				ttn_init(3, 50);
				//ttn_init(5, 45);

				// Set Vref DAC to 0V
				SPI2BUFL = spi_dac_reg(0, 1, 1, 1, 0);
				__delay_us(10);
				LATBbits.LATB6 = 0;		// Hold LDAC low
				__delay_us(5);			
				LATBbits.LATB6 = 1;		// Hold LDAC high

				// Set Peltier DAC to 0V
				SPI2BUFL = spi_dac_reg(1, 1, 1, 1, 0);
				__delay_us(10);
				LATBbits.LATB6 = 0;		// Hold LDAC low
				__delay_us(5);			
				LATBbits.LATB6 = 1;		// Hold LDAC high

				// Check TTN is connected and operational using temperature px
				ttn_rowcol(1,1);
				ttn_ramwrite(0);
				vs = 0;
				read = 0;
				// Sweep Vs from 0 to 1023 until vreg reached
				while(vs<1024 && read<vreg){							
					// Read pixel at Vs value
					ttn_pxvo(vs);
					vs++;
				}
				vs--;

//ttn_pxvo(1000);
//pxe_ffy(read);

				// Compare Vs,tau with optimal range
				if(vs>255 && vs<426){					
					// ACK and BT string end to Firefly
					U1TXREG = 'a';
				//	while(U1STAbits.TRMT==0);
					//pxe_ffy(end);
				}
				else{				
					// BAD RESULT and BT string end to Firefly
					U1TXREG = 'n';
					//while(U1STAbits.TRMT==0);
					//pxe_ffy(end);
				}

/*
				// Traverse all temperature px
				for(col_now=1; col_now<col_max; col_now+=3){
						ttn_col(col_now);
					for(row_now=1; row_now<row_max; row_now+=3){
						// Change pixel
						//ttn_rowcol(row, col);
ttn_row(row_now);
						ttn_ramwrite(0);
						vs = 0;
						read = 0;
						// Sweep Vs from 0 to 1023 until treg reached
						while((vs<1024) && (read < vreg)){						
							// Read pixel at Vs value
							ttn_pxvo(vs);
							vs++;
						}
						// Store correct Vs in RAM
						vs--;
						ttn_ramwrite(vs);
					}
				}
*/
				while(U1STAbits.TRMT==0);
				pxe_ffy(end);
				// Operation complete, deassert flag
				ttn_cmd = 0;
				LATBbits.LATB2 = 0;		// Turn off LED
			break;
			case 2:	// TAKE TEMPERATURE FOOTPRINT
				LATBbits.LATB2 = 1;		// Turn on LED				
				
				// Traverse all temperature px
				for(col_now=1; col_now<col_max; col_now+=3){
						ttn_col(col_now);
					for(row_now=1; row_now<row_max; row_now+=3){
						// Change pixel
						//ttn_rowcol(row, col);
ttn_row(row_now);
						ttn_ramwrite(0);
						vs = 0;
						read = 0;
						// Sweep Vs from 0 to 1023 until treg reached
						while((vs<1024) && (read < vreg)){						
							// Read pixel at Vs value
							ttn_pxvo(vs);
							vs++;
						}
						// Store correct Vs in RAM
						vs--;
						ttn_ramwrite(vs);
						// Send to Firefly
						pxe_ffy(vs);
					}
				}

				// Operation complete, deassert flag
				ttn_cmd = 0;
				// ACK and BT string end to Firefly
				U1TXREG = 'b';
				while(U1STAbits.TRMT==0);
				pxe_ffy(end);
				LATBbits.LATB2 = 0;		// Turn off LED
			break;
			case 3:	// SEARCH OPTIMAL REFERENCE ELEDTRODE VOLTAGE
				LATBbits.LATB2 = 1;		// Turn on LED

				int vmax, vmin;
				vref = 512;
				step = 512; // While-loop divides step by 2 at the start

				// Initialize at midpoint Vref = 0 (i.e. DAC 512/1023) 
				ctr_op = 0;
				SPI2BUFL = spi_dac_reg(0, 1, 1, 1, vref);
				dac_load();
				
				// Parse one frame to get initial ctr_op
				for(col_now=0; col_now<col_max; col_now++){
						ttn_col(col_now);
					for(row_now=0; row_now<row_max; row_now++){
						ttn_row(row_now);
						// Change pixel and clear RAM
						///ttn_rowcol(row, col);
						// Discharge behaviour of chemical px
						if((col_now%3!=1) || (row_now%3!=1)){
							ttn_ramwrite(0);
							// Read pixel Vout at Vs 0
							ttn_pxvoRef(0);
							vmin = read;
							// Read pixel Vout at Vs 700
							ttn_pxvoRef(800);
							vmax = read;
							
							// If Px is ON, increment counter
							if(vmin<100 && vmax>(vreg-tol)){
								ctr_op++;
							}	
						}				
					}					
				}

				// Binary search algorithm to maximize # of ON px
				while(step > 4){
	
					step /= 2;
			
					ctr_hi = 0;
					SPI2BUFL = spi_dac_reg(0, 1, 1, 1, vref+step);
					dac_load();
					// Parse one frame
					for(col_now=0; col_now<col_max; col_now++){
						ttn_col(col_now);
						for(row_now=0; row_now<row_max; row_now++){
						ttn_row(row_now);
							// Change pixel and clear RAM
							//ttn_rowcol(row, col);
							// Discharge behaviour of chemical px
							if((col_now%3!=1) || (row_now%3!=1)){
								ttn_ramwrite(0);
								// Read pixel Vout at Vs 0
								ttn_pxvoRef(0);
								vmin = read;
								// Read pixel Vout at Vs 700
								ttn_pxvoRef(800);
								vmax = read;
								
								// If Px is ON, increment counter
								if(vmin<100 && vmax>(vreg-tol)){
									ctr_hi++;
								}	
							}				
						}					
					}
			
					ctr_low = 0;		
					SPI2BUFL = spi_dac_reg(0, 1, 1, 1, vref-step);
					dac_load();
					// Parse one frame
					for(col_now=0; col_now<col_max; col_now++){
						ttn_col(col_now);
						for(row_now=0; row_now<row_max; row_now++){
						ttn_row(row_now);
							// Change pixel and clear RAM
							//ttn_rowcol(row, col);
							// Discharge behaviour of chemical px
							if((col_now%3!=1) || (row_now%3!=1)){
								ttn_ramwrite(0);
								// Read pixel Vout at Vs 0
								ttn_pxvoRef(0);
								vmin = read;
								// Read pixel Vout at Vs 700
								ttn_pxvoRef(800);
								vmax = read;
								
								// If Px is ON, increment counter
								if(vmin<100 && vmax>(vreg-tol)){
									ctr_low++;
								}	
							}				
						}					
					}
			
					// Adjust Vref and step size if current Vref is not optimal
					if(ctr_op<ctr_hi){
						vref += step;
						ctr_op = ctr_hi;
					}
					else if(ctr_op<ctr_low){
						vref -= step;
						ctr_op = ctr_low;
					}
				}

				count_pxon = ctr_op;

				// Set optimal Vref						
				SPI2BUFL = spi_dac_reg(0, 1, 1, 1, vref);
				//SPI2BUFL = spi_dac_reg(0, 1, 1, 1, 0);
				dac_load();
//ctr_op=0;
		// Store flags to indicate ON/OFF state of px and send to Firefly				
				for(col_now=0; col_now<col_max; col_now++){
						ttn_col(col_now);
					for(row_now=0; row_now<row_max; row_now++){
						ttn_row(row_now);
						// Change pixel and clear RAM
						//ttn_rowcol(row, col);

						// Only search on chemical px
						if((col_now%3==1) && (row_now%3==1)){
							// Read RAM value of temperature px
							ttn_ramread();
							vs = read;
						}
						else{ // Discharge behaviour of chemical px
							ttn_ramwrite(0);
							// Read pixel Vout at Vs 0
							ttn_pxvoRef(0);
						//	ttn_pxvo(0);
							vmin = read;
							//ttn_ramwrite(0);
							// Read pixel Vout at Vs 700
							ttn_pxvoRef(800);
						//	ttn_pxvo(700);
							vmax = read;
							
							// Determine if px is ON/OFF

							// Px ON, write 511 to RAM
							if(vmin<100 && vmax>(vreg-tol)){
								vs = 511; 
//ctr_op++;
							}
							// Px discharge too fast, write 0 to RAM
							else if(vmin<100 && vmax<100){
								vs = 0;
							}
							// Px discharge too slow, write 1023 to RAM
							else if(vmin>(vreg-tol) && vmax>(vreg-tol)){
								vs = 1023;
							}	
						}
						// Write flags to RAM
						ttn_ramwrite(vs);

						// Send to Firefly
						pxe_ffy(vs);				
					}					
				}
				

				// Operation complete, deassert flag
				ttn_cmd = 0;
				// ACK and BT string end to Firefly
				U1TXREG = 'd';
				while(U1STAbits.TRMT==0);
				pxe_ffy(end);
				LATBbits.LATB2 = 0;		// Turn off LED
			break;
			case 4:	// CALIBRATE ARRAY Vs VALUES	
				LATBbits.LATB2 = 1;		// Turn on LED
				int vsFlag;
				//int out1 = 0;
//count=0;
//count2=0;

				for(col_now=0; col_now<col_max; col_now++){	
						ttn_col(col_now);	
					for(row_now=0; row_now<row_max; row_now++){	
						ttn_row(row_now);	
						// Change pixel and clear RAM
						//ttn_rowcol(row, col);
						ttn_ramread();	
						vsFlag = read;

/*						// Only calibrate chemical px
						if((col_now%3==1) && (row_now%3==1)){
							vs = vsFlag;
						}	
						else{
							// Determine if px is ON/OFF
							// Ignore OFF px (RAM == 0 or 1023 from Vref step)
	 						if(vsFlag == 0b10101010){
count2++;
								vs = 0;
								//read = 0;
								int out1 = 0;
//out1 = 0;
								// Sweep Vs from 0 to 1023 until vreg reached
								while((vs<1024) && (out1 < (vreg-tol))){						
									// Read pixel at Vs value
									ttn_pxvo(vs);
									out1 = read;
if((out1-read) == 0){count++;}
									vs++;
								}
								// Correct Vs due to while-loop
								vs--;
							}
							else{ // OFF px
								vs = vsFlag;
							}
						}
*/

						// Only calibrate chemical px
						if(((col_now%3!=1) || (row_now%3!=1)) && (vsFlag == 511)){
							// Determine if px is ON/OFF
							// Ignore OFF px (RAM == 0 or 1023 from Vref step)	 						
							vs = 0;
int out1=0;
//count2++;
							//read = 0;
							// Sweep Vs from 0 to 1023 until vreg reached

							while((vs<1024) && (out1 < (vreg-tol))){
						
								// Read pixel at Vs value
								ttn_pxvo(vs);
								out1 = read;
								vs++;
							}
							// Correct Vs due to while-loop
							vsFlag = vs-1;	
						}


						// Write Vs to RAM
						ttn_ramwrite(vsFlag);
						// Send to Firefly
						pxe_ffy(vsFlag);				
					}					
				}

				// Operation complete, deassert flag
				ttn_cmd = 0;
				// ACK and BT string end to Firefly
				U1TXREG = 'e';
				while(U1STAbits.TRMT==0);
				// Firefly end frame
				pxe_ffy(end);
				LATBbits.LATB2 = 0;		// Turn off LED
			break;
			case 5:	// LOAD TEMPERATURE FOOTPRINT
				LATBbits.LATB2 = 1;		// Turn on LED
				__delay_ms(500);
				count = 0;
				// Traverse all temperature px
				for(col_now=1; col_now<col_max; col_now+=3){
						ttn_col(col_now);
					for(row_now=1; row_now<row_max; row_now+=3){
						ttn_row(row_now);
						// Change pixel
						//ttn_rowcol(row, col);
						// Store correct Vs in RAM
						vs = (tempfootprint[count] << 8) | tempfootprint[count+1];
						ttn_ramwrite(vs);
						count+=2;
					}
				}	

/*
__delay_ms(1000);		
				

tempnew = 0;
					ctr_temp = 0;
					// Traverse small region temperature px (assumed to be in contact with solution)
					for(col_now=37; col_now<53; col_now+=3){
						ttn_col(col_now);
						for(row_now=28; row_now<53; row_now+=3){
						ttn_row(row_now);
							ctr_temp++;
							// Change pixel
							//ttn_rowcol(row, col);
							// Extract Vs from RAM
							ttn_ramread();
							vs = read;
							// Read current OUT
							ttn_pxvo(vs);
							tempnew += read;
							// Restore the RAM Vs
							ttn_ramwrite(vs);
						}
					}
					tempavg = tempnew/ctr_temp;
tempnew = 0;
*/
				// Operation complete, deassert flag
				ttn_cmd = 0;
				// ACK and BT string end to Firefly
				U1TXREG = 'c';
				while(U1STAbits.TRMT==0);
				pxe_ffy(end);
				LATBbits.LATB2 = 0;		// Turn off LED
			break;
			case 6:	// REGULATE PELTIER TO 63C

				LATBbits.LATB2 = 1;		// Turn on LED
//count2 = 0;
/*
				int temptemp = (tau63[0] << 8) | tau63[1];
				if ((temptemp<200) && (temptemp>130)){
					tempref = temptemp;
				}
				else {tempref = 200;}
*/
				// Peltier DAC at full power 5V
//				SPI2BUFL = spi_dac_reg(1, 1, 1, 1, 500);
				SPI2BUFL = spi_dac_reg(1, 1, 1, 1, 1023);
				__delay_us(10);
				LATBbits.LATB6 = 0;		// Hold LDAC low
				__delay_us(5);			
				LATBbits.LATB6 = 1;		// Hold LDAC high
/*
tempnew = 0;
					ctr_temp = 0;
					// Traverse small region temperature px (assumed to be in contact with solution)
					for(col_now=37; col_now<53; col_now+=3){
						ttn_col(col_now);
						for(row_now=28; row_now<53; row_now+=3){
						ttn_row(row_now);
							ctr_temp++;
							// Change pixel
							//ttn_rowcol(row, col);
							// Extract Vs from RAM
							ttn_ramread();
							vs = read;
							// Read current OUT
							ttn_pxvo(vs);
							tempnew += read;
							// Restore the RAM Vs
							ttn_ramwrite(vs);
						}
					}
					tempavg = tempnew/ctr_temp;
tempnew = 0;
*/
				// Heat up until chip is slightly above 63C
				tempavg = vreg;
				//while(tempavg>(tempref-5)){
				while(tempavg>(tempref)){
					if(ttn_cmd == 8){
						// Turn off peltier
						SPI2BUFL = spi_dac_reg(1, 1, 1, 1, 0);
						__delay_us(10);
						LATBbits.LATB6 = 0;		// Hold LDAC low
						__delay_us(5);			
						LATBbits.LATB6 = 1;		// Hold LDAC high
						ttn_cmd = 0;
						goto stop;
					}
					tempnew = 0;
					ctr_temp = 0;
					// Traverse small region temperature px (assumed to be in contact with solution)
					for(col_now=37; col_now<53; col_now+=3){
						ttn_col(col_now);
						for(row_now=28; row_now<53; row_now+=3){
						ttn_row(row_now);
							ctr_temp++;
							// Change pixel
							//ttn_rowcol(row, col);
							// Extract Vs from RAM
							ttn_ramread();
							vs = read;
							// Read current OUT
							ttn_pxvo(vs);
							tempnew += read;
							// Restore the RAM Vs
							ttn_ramwrite(vs);
						}
					}
					tempavg = tempnew/ctr_temp;
				}

				// Operation complete, but maintain flag
				// ACK and BT string end to Firefly
				U1TXREG = 'f';
				while(U1STAbits.TRMT==0);
				pxe_ffy(end);
ttn_cmd = 0;
				LATBbits.LATB2 = 0;		// Turn off LED

				// Engage the PID controller to reach 63C and hold until 'play' is called
				// Ensure within 10-bit DAC limits
				temperr_prev = 0;
				while(ttn_cmd!=7){
count2++;
					if(ttn_cmd == 8){
						// Turn off peltier
						SPI2BUFL = spi_dac_reg(1, 1, 1, 1, 0);
						__delay_us(10);
						LATBbits.LATB6 = 0;		// Hold LDAC low
						__delay_us(5);			
						LATBbits.LATB6 = 1;		// Hold LDAC high
						ttn_cmd = 0;
						goto stop;
					}
					LATBbits.LATB2 = 1;		// Turn on LED
					__delay_ms(500);

					tempnew = 0;
					ctr_temp = 0;
					// Traverse small region temperature px (assumed to be in contact with solution)
					for(col_now=37; col_now<53; col_now+=3){
						ttn_col(col_now);
						for(row_now=28; row_now<53; row_now+=3){
						ttn_row(row_now);
							ctr_temp++;
							// Change pixel
							//ttn_rowcol(row, col);
							// Extract Vs from RAM
							ttn_ramread();
							vs = read;
							// Read current OUT
							ttn_pxvo(vs);
							tempnew += read;
							// Restore the RAM Vs
							ttn_ramwrite(vs);
						}
					}
					tempavg = tempnew/ctr_temp;

					// Error
					temperr = tempavg - tempref;// - 45;
	
					// Proportional
					tempP = tempPk*temperr;
					if(tempP>tdacmax/2){tempP=tdacmax/2;}
					else if(tempP<-(tdacmax/2)-1){tempP=-(tdacmax/2)-1;}
	
					// Integral
					tempI = tempPi*(temperr+temperr_prev);
					if(tempI>tdacmax/2){tempI=tdacmax/2;}
					else if(tempI<-(tdacmax/2)-1){tempI=-(tdacmax/2)-1;}
					
					// New DAC value
					tempdac =  tempP + tempI + (tdacmax/4); //+ tempD;
					if(tempdac>tdacmax){tempdac=tdacmax;}
					else if(tempdac<0){tempdac=0;}
					temperr_prev += temperr;
	
					// Send to DAC
					SPI2BUFL = spi_dac_reg(1, 1, 1, 1, tempdac);
					__delay_us(10);
					LATBbits.LATB6 = 0;		// Hold LDAC low
					__delay_us(5);			
					LATBbits.LATB6 = 1;		// Hold LDAC high
//tempdac0 = tempdac;

					if (count2>20){LATBbits.LATB2 = 0;}		// Turn off LED
					__delay_ms(500);
				}
				
				 
				 stop:  LATBbits.LATB2 = 0;		// Turn off LED

						
			break;
			case 7: // ARRAY READOUT
				temperr_prev /= 3;;
//	temperr_prev = 0;

				// Set optimal Vref						
				SPI2BUFL = spi_dac_reg(0, 1, 1, 1, (vref-50));
				__delay_us(10);
				LATBbits.LATB6 = 0;		// Hold LDAC low
				__delay_us(5);			
				LATBbits.LATB6 = 1;		// Hold LDAC high

				SPI2BUFL = spi_dac_reg(1, 1, 1, 1, tempdac);
				__delay_us(10);
				LATBbits.LATB6 = 0;		// Hold LDAC low
				__delay_us(5);			
				LATBbits.LATB6 = 1;		// Hold LDAC high

				while(ttn_cmd == 7){
if(ttn_cmd==0){break;}
					LATBbits.LATB2 = 1;		// Turn on LED

				
				//SPI2BUFL = spi_dac_reg(0, 1, 1, 1, 0);
				//dac_load();

				count = 0;
/*
				// Traverse all temperature px
				for(col_now=1; col_now<col_max; col_now+=3){
						ttn_col(col_now);
					for(row_now=1; row_now<row_max; row_now+=3){
						ttn_row(row_now);

						// Change pixel
//						ttn_ramread();
//						vs = read - 2;
//						ttn_ramwrite(vs);

						// Change pixel
						// Store correct Vs in RAM
						vs = ((tempfootprint[count] << 8) | tempfootprint[count+1]) - 3;// + 10;//25;
						ttn_ramwrite(vs);
						count+=2;

					}
				}

__delay_ms(1000);
*/
					for(col_now=0; col_now<col_max; col_now++){
						ttn_col(col_now);
						for(row_now=0; row_now<row_max; row_now++){
						ttn_row(row_now);
							// Chemical px
							if((col_now%3!=1) || (row_now%3!=1)){
								// Change pixel
								//ttn_rowcol(row, col);
								// Read and clear RAM Vs value
								ttn_ramread();
								vs = read;
								// Intermediate calib Vs of active px
								if(vs!=0 && vs!=1023){
										ttn_pxvo(vs);
										if(read<(vreg-tol) && vs<1020){
											while(read<(vreg-tol) && vs<1020){
												vs++;
												ttn_pxvo(vs);											
											};
											vs--;
										}
										else if(read>(vreg+tol) && vs>4){
											while(read>(vreg+tol) && vs>4){
												vs--;
												ttn_pxvo(vs);											
											};
											vs++;
										}
								}
								// Store Vs in RAM
								ttn_ramwrite(vs);
							}
							else{vs = 1023;} // Send 1023 if temperature px
							
							// Send value to Firefly
							pxe_ffy(vs);
						}
					}

					// Send Vref value
					pxe_ffy(vref);

					
					LATBbits.LATB2 = 0;		// Turn off LED	
	__delay_ms(8000);
//					__delay_ms(5000);
					
					tempnew = 0;
					ctr_temp = 0;
					int out2=0;
					// Traverse small region temperature px (assumed to be in contact with solution)
					for(col_now=37; col_now<53; col_now+=3){
						ttn_col(col_now);
						for(row_now=28; row_now<53; row_now+=3){
						ttn_row(row_now);
							ctr_temp++;
							// Change pixel
							//ttn_rowcol(row, col);
							// Extract Vs from RAM
							ttn_ramread();
							vs = read;
							// Read current OUT
							ttn_pxvo((vs-5));
out2=read;
							tempnew += out2;
							// Restore the RAM Vs
							ttn_ramwrite(vs);
						}
					}									
	
					// Compute mean temperature of this frame (cast to int)
					tempavg = (int)((double)tempnew/(double)ctr_temp);
					pxe_ffy(tempavg);
	
					// Firefly end frame	
					pxe_ffy(end);	
						
//	__delay_ms(1000);		
					// Engage PI Controller
/*
					// Error
					temperr = tempavg - tempref;// + 50;//(int)((double)55 * ((double)count_pxon/(double)ctr_op));
	
					// Proportional
					tempP = tempPkr*temperr;
					if(tempP>tdacmax/2){tempP=tdacmax/2;}
					else if(tempP<-(tdacmax/2)-1){tempP=-(tdacmax/2)-1;}

			 		// Deriative
					tempD = tempPd*(temperr-temperr_prev);
					if(tempD>tdacmax/2){tempD=tdacmax/2;}
					else if(tempD<-(tdacmax/2)-1){tempD=-(tdacmax/2)-1;}
					
					// Integral
					tempI = tempPir*(temperr+temperr_prev);
					if(tempI>tdacmax/2){tempI=tdacmax/2;}
					else if(tempI<-(tdacmax/2)-1){tempI=-(tdacmax/2)-1;}

					// New DAC value
					//tempdac = -(tdacmax/2) + tempP + tempD +tempI;
					tempdac = tempP + tempD +tempI;  
					if(tempdac>tdacmax){tempdac=tdacmax;}
					else if(tempdac<0){tempdac=0;}
					//temperr_prev += temperr;
					temperr_prev = temperr;
*/

					// Error
					temperr = tempavg - tempref;
	
					// Proportional
					tempP = tempPkr*temperr;
					//if(tempP>tdacmax){tempP=tdacmax;}
					//else if(tempP<-(tdacmax)-1){tempP=-(tdacmax)-1;}
	
					// Integral
					tempI = tempPir*(temperr+temperr_prev);
					//if(tempI>tdacmax){tempI=tdacmax;}
					//else if(tempI<-(tdacmax)-1){tempI=-(tdacmax)-1;}

					// Deriative
					tempD = tempPd*(temperr-temperr_prevD);
					//if(tempD>tdacmax){tempD=tdacmax;}
					//else if(tempD<-(tdacmax)-1){tempD=-(tdacmax)-1;}
					
					// New DAC value
					tempdac = tempP ; //+ tempI + tempD;// + tdacmax/4 ;
					if(tempdac>440){tempdac=440;}
					else if(tempdac<420){tempdac=420;}
					temperr_prev += temperr;
					temperr_prevD = temperr;				
					
	
					// Send to DAC
					SPI2BUFL = spi_dac_reg(1, 1, 1, 1, tempdac);
					__delay_us(10);
					LATBbits.LATB6 = 0;		// Hold LDAC low
					__delay_us(5);			
					LATBbits.LATB6 = 1;		// Hold LDAC high
					
					
					
					//__delay_ms(500);
					//__delay_ms(1000);

				}
	
				// Operation complete, deassert flag
				ttn_cmd = 0;
				LATBbits.LATB2 = 0;		// Turn off LED
			break;	

		}
	};
// END WHILE LOOP

	return 0;
}





/* RAM READ
for(col=0; col<col_max; col++){
					for(row=0; row<row_max; row++){						
						// Change pixel
						ttn_rowcol(row, col);	
						ttn_ramread();	
						pxe_ffy();			
					}					
				}
*/
/* DAC Sawtooth wave

	// Load new data into DAC input registers
	//while(!SPI2STATLbits.SPITBE);
	int b = 0;
	for (b=0; b<1024; b++){
	SPI2BUFL = spi_dac_reg(0, 1, 1, 1, b);
	__delay_us(10);
	//__delay_us(500);
	LATBbits.LATB6 = 0;		// Hold LDAC low
	__delay_us(5);			
	LATBbits.LATB6 = 1;		// Hold LDAC high
	}

*/


