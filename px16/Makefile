# MPLAB IDE generated this makefile for use with GNU make.
# Project: PX1.mcp
# Date: Fri Jun 08 16:38:45 2018

AS = xc16-as.exe
CC = xc16-gcc.exe
LD = xc16-ld.exe
AR = xc16-ar.exe
HX = xc16-bin2hex.exe
RM = rm

PX1.hex : PX1.cof
	$(HX) "PX1.cof" -omf=coff

PX1.cof : main.o
	$(CC) -omf=coff -mcpu=24FJ128GA202 "main.o" -o"PX1.cof" -Wl,-Tp24FJ128GA202.gld,--defsym=__MPLAB_BUILD=1,--defsym=__MPLAB_DEBUG=1,--defsym=__MPLAB_DEBUGGER_PK3=1,--defsym=__ICD2RAM=1,-Map="PX1.map",--report-mem

main.o : oscillator.h interrupts.h pins.h spi12.h uart1234.h initialize.h variables.h c:/program\ files\ (x86)/microchip/xc16/v1.31/support/generic/h/libpic30.h c:/program\ files\ (x86)/microchip/xc16/v1.31/include/lega-c/math.h c:/program\ files\ (x86)/microchip/xc16/v1.31/include/lega-c/string.h c:/program\ files\ (x86)/microchip/xc16/v1.31/include/lega-c/stdlib.h c:/program\ files\ (x86)/microchip/xc16/v1.31/include/lega-c/yvals.h c:/program\ files\ (x86)/microchip/xc16/v1.31/include/lega-c/stdio.h c:/program\ files\ (x86)/microchip/xc16/v1.31/include/lega-c/stdint.h c:/program\ files\ (x86)/microchip/xc16/v1.31/support/PIC24F/h/p24FJ128GA202.h c:/program\ files\ (x86)/microchip/xc16/v1.31/support/generic/h/xc.h config.h c:/program\ files\ (x86)/microchip/xc16/v1.31/support/PIC24F/h/p24FJ128GA202.h main.c
	$(CC) -omf=coff -mcpu=24FJ128GA202 -x c -c "main.c" -o"main.o" -D__DEBUG -D__MPLAB_DEBUGGER_PK3=1 -g -Wall

clean : 
	$(RM) "main.o" "PX1.cof" "PX1.hex"

