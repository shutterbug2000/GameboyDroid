package com.shutterbug.gameboy.Z80;
import java.io.*;
import android.util.*;
import java.util.logging.*;

public class Z80
{
	
	public int lastCycles;
	/**
	 * The 160x144 pixel 4 colors display
	 */
	private byte[] display;
	/**
	 * 64K Memory<br/>
	 * 0000-3FFF 16KB ROM Bank 00 (in cartridge, fixed at bank 00)<br/>
	 * 4000-7FFF 16KB ROM Bank 01..NN (in cartridge, switchable bank number)<br/>
	 * 8000-9FFF 8KB Video RAM (VRAM) (switchable bank 0-1 in CGB Mode)<br/>
	 * A000-BFFF 8KB External RAM (in cartridge, switchable bank, if any)<br/>
	 * C000-CFFF 4KB Work RAM Bank 0 (WRAM)<br/>
	 * D000-DFFF 4KB Work RAM Bank 1 (WRAM) (switchable bank 1-7 in CGB Mode)<br/>
	 * E000-FDFF Same as C000-DDFF (ECHO) (typically not used)<br/>
	 * FE00-FE9F Sprite Attribute Table (OAM)<br/>
	 * FEA0-FEFF Not Usable<br/>
	 * FF00-FF7F I/O Ports<br/>
	 * FF80-FFFE High RAM (HRAM)<br/>
	 * FFFF Interrupt Enable Register<br/>
	 */
	public char[] memory;
	/**
	 * 8 8-bit Registers<br/>
	 * They can be paired up to 16-bit registers<br/>
	 * Left 8-bit registers can be: A, B, D and H<br/>
	 * Register F is the "Flag Register"<br/>
	 * The flag register contains multiple flags<br/>
	 * Bit 7: Zero flag (Z)<br/>
	 * Bit 6: Subtract Flag (N)<br/>
	 * Bit 5: Half Carry Flag (H)<br/>
	 * Bit 4: Carry Flag (C)<br/>
	 */
	private char[] register;
	/**
	 * The StackPointer
	 */
	private char sp;
	/**
	 * The ProgramCounter<br/>
	 * The PC is being initiated on startup to the position: 0x100<br/>
	 * At that location is commenly a jump and a nop
	 */
	private char pc;

	private boolean needRedraw;

	/**
	 * Resets the Z80 processor
	 */
	public void reset() {
		display = new byte[160 * 144];
		memory = new char[65535];
		register = new char[8];
		pc = 0x100;
		sp = 0xFFFE;
		}
		
		public void opcode(){
			switch(memory[pc]){
				case 0x00:{ //nop
					pc++;
					lastCycles = 4;
					break;
				}
				
				case 0x0C:{
					incrementReg(Register.C);
					incrementPc();
					cycles(4);
					break;
				}
				
				case 0x0E:{
					loadNumber(Register.C);
					incrementPc();
					cycles(8);
					break;
				}
				
				case 0x18:{ // jr, n
					writeMemory(pc, readMemory(pc + 1));
					incrementPc();
					cycles(8);
					break;
				}
				
				case 0x19:{
					add16bit(Register.D, Register.E);
					incrementPc();
					cycles(8);
					break;
				}
				
				case 0x1B:{
					decrement16BitReg(Register.D, Register.E);
					incrementPc();
					cycles(8);
					break;
				}
				
				case 0x1C:{
					decrementReg(Register.E);
					incrementPc();
					cycles(4);
					break;
				}
				
				case 0x1D:{
					decrementReg(Register.D);
					incrementPc();
					cycles(4);
					break;
				}
				
				case 0x25:{
					decrementReg(Register.H);
					incrementPc();
					cycles(4);
					break;
				}
				
				case 0x2C:{ // inc l
						incrementReg(Register.L);
						setFlagRegister(Flag.Subtract, false);
						testZero(Register.L);
						testHalfCarry(Register.L);
						incrementPc();
						cycles(4);
						break;
					}
				
				case 0x4A:{
					load(Register.C, Register.D);
					incrementPc();
					cycles(4);
					break;
				}
				
				
				case 0x4B:{
					load(Register.C, Register.E);
					incrementPc();
					cycles(4);
					break;
				}
				
				case 0x51:{
					setRegister(Register.C, getRegister(Register.D));
					incrementPc();
					cycles(4);
					break;
				}
				
				case 0x52:{
					incrementPc();
					cycles(4);
					break;
				}
				
				case 0x53:{
				load(Register.D, Register.E);
				incrementPc();
				cycles(4);
				break;
				}
				
				case 0x55:{
					load(Register.D, Register.L);
					incrementPc();
					cycles(4);
					break;
				}
				
				case 0x56:{
					loadHl2(Register.D);
					incrementPc();
					cycles(8);
					break;
					}
				
					case 0x58:{
						load(Register.E, Register.B);
						incrementPc();
						cycles(4);
						break;
					}
					
					case 0x6D:{
						incrementPc();
						cycles(4);
						break;
					}
					
					case 0x6E:{
						loadHl2(Register.L);
						incrementPc();
						cycles(4);
						break;
					}
					
					case 0x70:{
						loadHl(Register.B);
						incrementPc();
						cycles(8);
						break;
					}
					
				case 0x87:{ //add a,a
					int setToA = getRegister(Register.A) + getRegister(Register.A);
					setRegister(Register.A, setToA);
					testZero(Register.A);
					setFlagRegister(Flag.Subtract, false);
					testCarry(Register.A);
					testHalfCarry(Register.A);
					incrementPc();
					cycles(4);
					break;
				}
				
				case 0xC3:{ // jp nn 
					pc = (char)( readMemory(pc) | (readMemory(pc + 1) << 8));
					lastCycles = 12;
					break;
				}
				
				case 0xE1:{ //pop hl
					setRegister(Register.L, (char)pop());
					setRegister(Register.H, (char)pop());
					cycles(12);
					break;
				}
				
				case 0xFF:{
					pc++;
					sp -= 2;
					writeMemory(sp, (pc >> 8) & 0xFF);
					writeMemory(sp, pc & 0x00FF);
					pc = 0x38;
					lastCycles = 32;
					Log.d("d3bug3d", Integer.toHexString(memory[pc]));
					break;
				}
				default:{
						Log.d("Unknown Z80 opcode...","Unknown Z80 opcode... " + Integer.toHexString(memory[pc]) + " ...DEBUGGING TIME");
						System.exit(0);
						}
			}
		}
	
	public void loadCartridge(String file) {
		DataInputStream input = null;
		try {
			input = new DataInputStream(new FileInputStream(new File(file)));

			int offset = 0;
			while (input.available() > 0) {
				memory[offset] = (char) (input.readByte() & 0xFF);
				offset++;
			}

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException ex) {
				}
			}
		}
		}
		
	public int getRegister(Register register) {
		return this.register[register.index];
	}
	
	public int getRegister(Register leftRegister, Register rightRegister) {
		return (register[leftRegister.index] << 8) | register[rightRegister.index];
	}
		
	public void setRegister(Register register, int value) {
		this.register[register.index] = (char)(value & 0xFF);
	}
	
	public void writeMemory(int address, int value) {
		memory[address] = (char)(value & 0xFF);
	}
	public void incrementPc(){
		pc++;
	}
	
	public void incrementPcBy(int inc){
		pc += inc;
	}
	
	public void incrementSpBy(int inc){
		sp += inc;
	}
	
	public void cycles(int cycles){
		this.lastCycles = cycles;
	}
	
	public int readMemory(int address) {
		return memory[address] & 0xFFFF;
		}
	
	public void setRegister(Register leftRegister, Register rightRegister, int value) {
		register[leftRegister.index] = (char)((value >>> 8) & 0xFF);
		register[rightRegister.index] = (char)(value & 0xFF);
		}
	public int pop(){
			sp++;
			return memory[sp];
		}
		
	public void setFlagRegister(Flag flag, boolean set) {
		if(set) {
			//Turn flag on
			register[Register.F.index] |= 1 << flag.bit;
		} else {
			//Turn flag off
			register[Register.F.index] &= ~(1 << flag.bit);  
		}
		}
		
		public void testZero(Register r){
			if(getRegister(r) == 0){
				setFlagRegister(Flag.Zero, true);
			} else if(getRegister(r) >= 1){
				setFlagRegister(Flag.Zero, false);
			}
			}
			
			public void testCarry(Register r){
				char var = 0xFFFF; // or u8 var=0xFF;
				int value = getRegister(r);
				if ((var+value) < var){
				setFlagRegister(Flag.Carry, true);
				} else {
					setFlagRegister(Flag.Carry, false);
				}
				}
				public void testHalfCarry(Register r){
					int var = 0xFF;
					int value = getRegister(r);
					if (((var & 0xF) + value) < (var & 0xF)){
						setFlagRegister(Flag.HalfCarry, true);
						} else {
							setFlagRegister(Flag.HalfCarry, false);
						}
				}
				
	public void test16bitCarry(Register l, Register r){
		char var = 0xFFFF; // or u8 var=0xFF;
		int value = getRegister(l, r);
		if ((var+value) < var){
			setFlagRegister(Flag.Carry, true);
		} else {
			setFlagRegister(Flag.Carry, false);
		}
	}
	public void test16bitHalfCarry(Register l, Register r){
		int var = 0xFF;
		int value = getRegister(l, r);
		if (((var & 0xF) + value) < (var & 0xF)){
			setFlagRegister(Flag.HalfCarry, true);
		} else {
			setFlagRegister(Flag.HalfCarry, false);
		}
	}
				
				public void incrementReg(Register r){
					int initVal = getRegister(r);
					setRegister(r, initVal++);
				}
				
				public void decrementReg(Register r){
					int initVal = getRegister(r);
					setRegister(r, initVal--);
					}
				
	public void increment16BitReg(Register l, Register r){
		int initVal = getRegister(l, r);
		setRegister(l, r, initVal++);
	}
	public void decrement16BitReg(Register l, Register r){
		int initVal = getRegister(l, r);
		setRegister(l,r, initVal--);
		}
					
				public void load(Register r1, Register r2){
					setRegister(r1, getRegister(r2));
				}
				public void loadNumber(Register r){
					setRegister(r, readMemory(pc++));
					}
				
					public void loadHl(Register r){
						writeMemory(getRegister(Register.H, Register.L), getRegister(r));
						}
				
						public void loadHl2(Register r){
							int set = readMemory(getRegister(Register.H, Register.L));
							setRegister(r, set);
							}
						
				public void add16bit(Register l, Register r){
					int setTo = getRegister(l, r) + getRegister(l,r);
					setRegister(Register.H, Register.L, setTo);
					setFlagRegister(Flag.Subtract, false);
					test16bitCarry(l, r);
					test16bitHalfCarry(l, r);
				}
				}
