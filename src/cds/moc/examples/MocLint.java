// Copyright 2011 - Unistra/CNRS
// The MOC API project is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of MOC API java project.
//
//    MOC API java project is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    MOC API java project is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with MOC API java project.
//
package cds.moc.examples;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;

import cds.moc.HealpixMoc;


/**
 * MOC check - verifying the IVOA 1.0 MOC recommendation compatibility
 * @author P.Fernique [CDS]
 * @version 1.0 - April 2016
 *
 */
public class MocLint {

   
   static final private int MAXERROR = 20;
   
   private static int error(StringBuilder out,String s) {
      out.append("ERROR   "+s+"\n");
      return 1;
   }
   
   private static void info(StringBuilder out,String s) {
      out.append("INFO    "+s+"\n");
   }
   
   private static void status(StringBuilder out,String s) {
      out.append("STATUS  "+s+"\n");
   }
   
   private static int warning(StringBuilder out,String s) {
      out.append("WARNING "+s+"\n");
      return 1;
   }
   
   private static void tooMany(StringBuilder out) throws Exception {
      out.append("ERROR   Too many errors. Is it really a MOC ?"+"\n");
      throw new Exception();
   }
   
   // Example: 2016-05-09[T10:39[:00]]
   private static boolean checkDate(String s) {
      int mode=0;
      for( int i=0; i<s.length(); i++ ) {
         char ch = s.charAt(i);
         switch(mode) {
            case 0: if( ch=='-' ) mode=1;
                    else if( !Character.isDigit(ch) ) return false;
                    break;
            case 1: if( ch=='-' ) mode=2;
                    else if( !Character.isDigit(ch) ) return false;
                    break;
            case 2: if( ch=='T' ) mode=3;
                    else if( !Character.isDigit(ch) ) return false;
                    break;
            case 3: if( ch==':' ) mode=4;
                    else if( !Character.isDigit(ch) ) return false;
                    break;
            case 4: if( ch==':' ) mode=5;
                    else if( !Character.isDigit(ch) ) return false;
                    break;
            case 5: if( !Character.isDigit(ch) ) return false;
                    break;
         }
      }
      return mode==2 || mode==4 || mode==5;
   }
   
   // Extract FITS value from FITS header line. Remove the quotes if required
   private static String getVal(byte [] buffer) {
      int i;
      boolean quote = false;
      boolean blanc=true;
      int offset = 9;

      for( i=offset ; i<80; i++ ) {
         if( !quote ) {
            if( buffer[i]==(byte)'/' ) break;   // on a atteint le commentaire
         } else {
            if( buffer[i]==(byte)'\'') break;   // on a atteint la prochaine quote
         }

         if( blanc ) {
            if( buffer[i]!=(byte)' ' ) blanc=false;
            if( buffer[i]==(byte)'\'' ) { quote=true; offset=i+1; }
         }
      }
      return (new String(buffer, 0, offset, i-offset)).trim();
  }
   
   // Extract FITS keyword from FITS header line.
   private static String getKey(byte [] buffer) {
      return new String(buffer, 0, 0, 8).trim();
   }

   // Convert s in integer
   private static long getInt(String s) {
      long v;
      try {
         v = Long.parseLong(s);
         return v;
      } catch( Exception e) { }
      return -1;
   }
   
   /** Check the IVOA 1.0 MOC recommendation compatibility
    * @param in stream containing the MOC in FITS container
    * @return true if MOC is compatible
    */
   public static boolean check(InputStream in) {
      StringBuilder out = new StringBuilder();
      int rep = check( out,in);
      System.out.print(out.toString());
      return rep!=0;
   }
   
   /**
    * Check the IVOA 1.0 MOC recommendation compatibility
    * @param out Trace of the validator
    * @param in stream containing the MOC in FITS container
    * @return 1-ok, 0-error, -1-warning
    */
   public static int check(StringBuilder out,InputStream in) {
      BufferedInputStream bis=null;
      long naxis = -1, naxis1 = -1, naxis2 = -1, pcount = 0, gcount = 1, tfields = -1, mocorder = -1;
      String tform1 = "", ttype1 = "", pixtype = "", ordering = "", coordsys = "";
      String moctool = "", date = "", origin = "", moctype = "", mocid = "", extname = "";
      int w=0,e=0;

      try {
         bis = new BufferedInputStream(in, 32 * 1024);
         byte buf[] = new byte[80];
         int n;
         long bitpix = -1;
         String extend = "";
         int line = 0;
         int size = 0;
         
         // Reading first FITS HDU
         while( (n = bis.read(buf)) != 0 ) {
            size += n;
            line++;
            if( buf[0] == 'E' && buf[1] == 'N' && buf[2] == 'D' ) break;
            String key = getKey(buf);
            if( key.equals("COMMENT") || key.equals("HISTORY") ) continue;
            String val = getVal(buf);
            String s = (new String(buf,0,0,n)).trim();
            if( ((char) buf[8]) != '=' ) {
               e+=error(out,"[2.3.2] HDU0 line " + line + ": missing \"=\" character ["+s+"]");
            }
            if( line == 1 && (!key.equals("SIMPLE") || key.equals("SIMPLE") && !val.equals("T")) ) {
               e+=error(out,"[2.3.2] HDU0 line "+ line + ": SIMPLE=T missing ["+s+"]");
            }
            //         if( key.equals("BITPIX") ) bitpix=getInt(val); 
            if( key.equals("EXTEND") ) extend = val;
            //         out.println("["+key+"] ["+val+"]");
            if( e>MAXERROR ) tooMany(out);
         }
         //      if( bitpix!=8 ) e+=error(out,"FITS error: BITPIX=8 required in primary HDU");
         if( !extend.equals("T") ) {
            w+=warning(out,"[2.3.2] HDU0: EXTEND=T required");
         }
         
         // Skipping end of primary HDU
         int skip = 2880 - size % 2880;
         if( skip != 2880 ) {
            size += bis.skip(skip);
         }
         // Reading the second HDU
         line = 0;
         while( (n = bis.read(buf)) != 0 ) {
            size += n;
            line++;
            if( buf[0] == 'E' && buf[1] == 'N' && buf[2] == 'D' ) break;
            String key = getKey(buf);
            if( key.equals("COMMENT") || key.equals("HISTORY") ) continue;
            String val = getVal(buf);
            String s = (new String(buf,0,0,n)).trim();
            if( ((char) buf[8]) != '=' ) {
               e+=error(out,"[2.3.2] HDU1 line " + line + ": missing \"=\" character ["+s+"]");
            }
            if( line == 1 && (!key.equals("XTENSION") || key.equals("XTENSION") && !val.equals("BINTABLE")) ) {
               e+=error(out,"[2.3.2] HDU1 line " + line + ": XTENSION=BINTABLE missing");
            }
            if( key.equals("BITPIX") ) bitpix = getInt(val);
            else if( key.equals("NAXIS") ) naxis = getInt(val);
            else if( key.equals("NAXIS1") ) naxis1 = getInt(val);
            else if( key.equals("NAXIS2") ) naxis2 = getInt(val);
            else if( key.equals("PCOUNT") ) pcount = getInt(val);
            else if( key.equals("GCOUNT") ) gcount = getInt(val);
            else if( key.equals("TFIELDS") ) tfields = getInt(val);
            else if( key.equals("MOCORDER") ) mocorder = getInt(val);
            else if( key.equals("TFORM1") ) tform1 = val;
            else if( key.equals("TTYPE1") ) ttype1 = val;
            else if( key.equals("PIXTYPE") ) pixtype = val;
            else if( key.equals("ORDERING") ) ordering = val;
            else if( key.equals("COORDSYS") ) coordsys = val;
            else if( key.equals("MOCTOOL") ) moctool = val;
            else if( key.equals("DATE") ) date = val;
            else if( key.equals("ORIGIN") ) origin = val;
            else if( key.equals("EXTNAME") ) extname = val;
            else if( key.equals("MOCTYPE") ) moctype = val;
            else if( key.equals("MOCID") ) mocid = val;
//            out.println("[" + key + "] [" + val + "]");
            if( e>MAXERROR ) tooMany(out);
         }
         
         if( moctool.length()>0 ) info(out,"Generated by: "+moctool);
         if( date.length()>0 )    info(out,"Date: "+date);
         if( origin.length()>0 )  info(out,"Origin: "+origin);
         if( mocid.length()>0 )   info(out,"Moc id: "+mocid);
         if( extname.length()>0 ) info(out,"Extname: "+extname);
         if( moctype.length()>0 ) info(out,"Moc type: "+moctype);
         if( mocorder!=-1 )       info(out,"Moc order: "+mocorder);
         if( naxis2!=-1 )         info(out,"Number of rows: "+naxis2);
         if( tform1.length()>0 )  info(out,"Coding: " +(tform1.endsWith("J")?"32 bits integer":tform1.endsWith("K")?"64 bits long":tform1));

         if( coordsys.length()==0 ) w+=warning(out,"[2.3.2a]: COORDSYS=C mandatory in HDU1");
         if( !pixtype.equals("HEALPIX") ) w+=warning(out,"[2.3.2a]: PIXTYPE=HEALPIX mandatory in HDU1");
         if( !ordering.equals("NUNIQ") ) w+=warning(out,"[2.3.2a]: ORDERING=NUNIQ mandatory in HDU1");
         if( gcount != 1 ) w+=warning(out,"[2.3.2]: only GCOUNT=1 authorized in HDU1");
         if( pcount != 0 ) w+=warning(out,"[2.3.2]: only PCOUNT=0 authorized in HDU1");
         if( mocorder ==-1 ) e+=error(out,"[2.3.2b]: MOCORDER is mandatory in HDU1");
         else if( mocorder < 0 || mocorder > 29 ) e+=error(out,"[2.3.2b]: MOCORDER=n where n in [0..29] required in HDU1");
         if( mocorder==29 ) info(out,"(!) mocOrder 29 is probably a wrong default value rather than a deliberated choice - check it!");
         if( tfields != 1 ) e+=error(out,"[2.3.2]: TFIELDS=1 required in HDU1");
         if( tform1.length() > 1 && tform1.charAt(0) != '1' ) e+=error(out,"[2.3.2]: TFORM1=1J or 1K required in HDU1");
         if( tform1.length() > 1 ) tform1 = tform1.substring(1);
         if( !tform1.equals("J") && !tform1.equals("K") ) e+=error(out,"[2.3.2]: TFORM1=1J or 1K required in HDU1");
         if( tform1.equals("J") && mocorder > 13 ) info(out,"(!) mocOrder>13 may require 64 rather than 32 bits integer coding (TFORM1=1K) - check it!");
         if( naxis != 2 ) e+=error(out,"[2.3.2]: only NAXIS=2 authorized in HDU1");
         if( coordsys.length()>0 && !coordsys.equals("C") ) w+=warning(out,"[2.2.1]: wrong COORDSYS ["+coordsys+"]. MOC must use ICRS (C) only");
         if( tform1.equals("J") && naxis1 != 4 ) e+=error(out,"[2.3.2]: only NAXIS1=4 compatible with TFORM1=J in HDU1");
         if( tform1.equals("K") && naxis1 != 8 ) e+=error(out,"[2.3.2]: only NAXIS1=8 compatible with TFORM1=K in HDU1");
         if( naxis2 < 0 ) e+=error(out,"[2.3.2]: NAXIS2 error in HDU1");
         if( date.length()>0 && !checkDate(date) ) w+=warning(out,"[2.3.2]: DATE syntax error: no FITS convention ["+date+"]");
         
         // Skipping end of secondary HDU
         skip = 2880 - size % 2880;
         if( skip != 2880 ) {
            size += bis.skip(skip);
         }
         
         // Reading binary elements
         HealpixMoc moc = new HealpixMoc((int)mocorder);
         moc.setCheckConsistencyFlag(false);
         long[] hpix = null;
         int nbyte = tform1.equals("J") ? 4 : 8;
         byte t[] = new byte[nbyte];
         for( long i = 0; i < naxis2; i++ ) {
            n=0;
            int m1;
            while( (m1=bis.read(t,n,t.length-n))!=0 ) { n+=m1; if( n==t.length ) break; }
            size += n;
            if( n != t.length ) e+=error(out,"[2.3.2]: truncated FITS table after row " + i);

            long val = 0;
            int a = ((t[0]) << 24) | (((t[1]) & 0xFF) << 16) | (((t[2]) & 0xFF) << 8) | (t[3]) & 0xFF;
            if( nbyte == 4 ) val = a;
            else {
               int b = ((t[4]) << 24) | (((t[5]) & 0xFF) << 16) | (((t[6]) & 0xFF) << 8) | (t[7]) & 0xFF;
               val = (((long) a) << 32) | ((b) & 0xFFFFFFFFL);
            }
            hpix = HealpixMoc.uniq2hpix(val, hpix);
            int order = (int) hpix[0];
            long npix = hpix[1];
            if( order < 0 || order > 29) e+=error(out,"[2.3.1]: order error in row " + i+ " ["+order+"]");
            if( mocorder>=0 && order > mocorder ) w+=warning(out,"[2.3.1]: order greater than mocorder in row " + i+ " ["+order+"]");
            long maxnpix = HealpixMoc.pow2(order);
            maxnpix *= maxnpix;
            maxnpix *= 12L;
            if( npix < 0 ) e+=error(out,"[2.3.1]: npix negative error in row " + i+ " ["+npix+"]");
            if( npix >= maxnpix ) e+=error(out,"[2.3.1]: npix too high for the current order in row " + i+ " ["+npix+"]");
            moc.add(order, npix);
            if( e>MAXERROR ) tooMany(out);
         }
         
         // Bien formée ?
         String s = moc.toString();
         moc.setCheckConsistencyFlag(true);
         if( !s.equals(moc.toString()) ) w+=warning(out,"[2.2.3]: not well formed");
         
         // Finishing FITS stream
         skip = 2880 - size % 2880;
         if( skip != 2880 ) {
            n = (int) bis.skip(skip);
            size+=n;
            if( n < skip ) w+=warning(out,"[2.3.2]: FITS not aligned on 2880 byte blocks");
         }
         bis.close();
         bis=null;
         info(out,"FITS size: "+size+" bytes");
      } catch( Exception e1 ) {
         e+=error(out,"Unrecovered exception !");
      } finally {
         if( bis!=null ) try { bis.close(); } catch( Exception e2 ) {}
      }
      
      if( w==0 && e==0 ) {
         status(out,"OK! MOC compatible with IVOA MOC 1.0 recommendation");
         return 1;
      } else if( e==0 ) {
         status(out,"WARNING! MOC ok but not fully compatible with IVOA MOC 1.0 recommendation");
         return -1;
      } else {
         status(out,"ERROR! MOC error, not usable");
         return 0;
      }
   }
   
   /** Check the IVOA 1.0 MOC recommendation compatibility
    * @param filename name of the file containing the MOC in FITS container
    * @return true if MOC is compatible
    */
   public static boolean check(String filename) throws Exception {
      FileInputStream in = null;
      try {
         in = new FileInputStream(filename);
         return check(in);
      } finally { if( in!=null ) in.close(); }
   }
   
   // Just for test
   public static void main(String[] args) {
      try { check(args[0]); }
      catch( Exception e ) { e.printStackTrace(); }
   }

}
