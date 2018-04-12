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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import cds.moc.Healpix;
import cds.moc.HealpixMoc;
import cds.moc.MocCell;

public class MocTest {


   // Juste pour tester quelques méthodes */
   private static boolean testBasic() throws Exception {
      title("testBasic: Create a Moc manually and check the result...");
      String ref = " 3/1 3/3 3/10 4/16 4/17 4/18 4/22";
      HealpixMoc moc = new HealpixMoc();
      moc.add("3/10 4/12-15 18 22");
      moc.add("4/13-18 5/19 20");
      moc.add("3/1");
      moc.sort();
      Iterator<MocCell> it = moc.iterator();
      StringBuffer s = new StringBuffer();
      while( it.hasNext() ) {
         MocCell p = it.next();
         s.append(" "+p.order+"/"+p.npix);
      }
      boolean rep = s.toString().equals(ref);
      if( !rep ) {
         System.out.println("MocTest.testBasic ERROR: \n.get ["+s+"]\n.ref ["+ref+"]\n");
      } else System.out.println("MocTest.testBasic OK");
      return rep;
   }
   
   private static boolean testSetLimitOrder() throws Exception {
      title("testSetLimitOrder: Test min and max limit order settings...");
      HealpixMoc moc = new HealpixMoc("0/0 3/700 8/");
      String ref= "{ \"1\":[0,1,2,3], \"2\":[175] }";
      int mocOrder=2;
      
      System.out.println("MOC before: "+moc);
      moc.setMinLimitOrder(1);
      moc.setMocOrder(2);
      System.out.println("MOC order [1..2]: "+moc);
      
      if( !moc.toString().equals(ref) ) {
         System.out.println("MocTest.testSetLimitOrder ERROR\n");
         return false;
      }
      
      if( moc.getMocOrder()!=mocOrder ) {
         System.out.println("MocTest.testSetLimitOrder ERROR: wrong mocOrder "+moc.getMocOrder()+" waiting "+mocOrder);
         return false;
      }
      
      // Même test mais en ajoutant les pixels un par un
      moc = new HealpixMoc();
      moc.setMinLimitOrder(1);
      moc.setMocOrder(2);
      moc.setCheckConsistencyFlag(false);
      moc.add(0,0);
      moc.add(4,2810);
      moc.setCheckConsistencyFlag(true);
      if( !moc.toString().equals(ref) ) {
         System.out.println("MocTest.testSetLimitOrder ERROR\n");
         return false;
      }

      
      System.out.println("testSetLimitOrder OK");
      return true;
   }

   private static boolean testIterativeInsertion() throws Exception {
      long maxIns= 1000010L;
      title("testIterativeInsertion: Test various npix insertion methods ("+maxIns+" insertions)...");
      long t0,t1;
      HealpixMoc moc,moc1;
      
      // Préparation des valeurs aléatoires
      int maxOrder=15;
      int randOrder[] = new int[maxOrder];
      long randNpix[][] = new long[maxOrder][];
      long maxNpix = HealpixMoc.pow2(maxOrder); maxNpix*=maxNpix*12;
      for( int i=0; i<randOrder.length; i++ ) {
         randOrder[i] = (int)(Math.random()*maxOrder);
         long max = HealpixMoc.pow2(i); max*=12L*max;
         randNpix[i] = new long[(int)(max>50000?50000:max)];
         for( int j=0; j<randNpix[i].length; j++ ) randNpix[i][j] = (long)(Math.random()*max);
      }
      
      // Insertion bas niveau avec vérif de cohérence systématique
      moc = new HealpixMoc();
      t0=System.currentTimeMillis();
      for( long i=0; i<maxIns; i++ ) {
         int order = randOrder[(int)(i%randOrder.length)];
         long npix = randNpix[order][(int)(i%randNpix[order].length)];
         moc.add(order,npix);
      }
      t1=System.currentTimeMillis();
      System.out.println(".insertion with systematical check: "+(t1-t0)+"ms");
      
      // Insertion bas niveau avec vérif de cohérence systématique
      moc1 = new HealpixMoc();
      t0=System.currentTimeMillis();
      int nbadds=0;
      moc1.setCheckConsistencyFlag(false);
      for( long i=0; i<maxIns; i++ ) {
         int order = randOrder[(int)(i%randOrder.length)];
         long npix = randNpix[order][(int)(i%randNpix[order].length)];
         moc1.add(order,npix);
         if( nbadds>100000 ) { moc1.checkAndFix(); nbadds=0; }
         nbadds++;
      }
      moc1.setCheckConsistencyFlag(true);
      t1=System.currentTimeMillis();
      System.out.println(".insertion with manual checks (each 100000 insertions): "+(t1-t0)+"ms");
      
      if( !moc.equals(moc1) ) {
         System.out.println("MocTest.testIterativeInsertion ERROR: inconsistency results A:\n.moc::"+moc.todebug()+"\n.moc1:"+moc1.todebug());
         return false;
      }
      
      // Préparation de collections et de arrays
      ArrayList<Long> [] mem = new ArrayList[maxOrder];
      for( int i=0; i<maxOrder; i++ ) mem[i] = new ArrayList<Long>();
      long [][] memA = new long[maxOrder][];
      for( long i=0; i<maxIns; i++ ) {
         int order = randOrder[(int)(i%randOrder.length)];
         long npix = randNpix[order][(int)(i%randNpix[order].length)];
         mem[order].add(npix);
      }
      for( int i=0; i<maxOrder; i++ ) {
         memA[i] = new long[ mem[i].size() ];
         for( int j=0; j<mem[i].size(); j++ ) memA[i][j] = mem[i].get(j);
      }
      
      // Insertion par Collections de longs
      moc1 = new HealpixMoc();
      t0=System.currentTimeMillis();
      for( int i=0; i<maxOrder; i++ ) moc1.add(i,mem[i]);
      t1=System.currentTimeMillis();
      System.out.println(".insertion by Collection of Longs: "+(t1-t0)+"ms");
      
      if( !moc.equals(moc1) ) {
         System.out.println("MocTest.testIterativeInsertion ERROR: inconsistency results B:\n.moc::"+moc.todebug()+"\n.moc1:"+moc1.todebug());
         return false;
      }

      // Insertion par tableaux de longs
      moc1 = new HealpixMoc();
      t0=System.currentTimeMillis();
      for( int i=0; i<maxOrder; i++ ) moc1.add(i,memA[i]);
      t1=System.currentTimeMillis();
      System.out.println(".insertion by arrays of longs: "+(t1-t0)+"ms");
      
      if( !moc.equals(moc1) ) {
         System.out.println("MocTest.testIterativeInsertion ERROR: inconsistency results C:\n.moc::"+moc.todebug()+"\n.moc1:"+moc1.todebug());
         return false;
      }
      
      // Insertion par lecture d'un MOC pré-existant en FITS
      String filename = "MocTmp.fits";
      moc.write(filename);
      moc1 = new HealpixMoc();
      t0=System.currentTimeMillis();
      moc1.read(filename);
      t1=System.currentTimeMillis();
      System.out.println(".insertion from FITS file (no check): "+(t1-t0)+"ms");
      (new File(filename)).delete();
      if( !moc.equals(moc1) ) {
         System.out.println("MocTest.testIterativeInsertion ERROR: inconsistency results D:\n.moc::"+moc.todebug()+"\n.moc1:"+moc1.todebug());
         return false;
      }

      // Insertion par lecture d'un MOC pré-existant en FITS
      filename = "MocTmp.json";
      moc.write(filename,HealpixMoc.JSON);
      moc1 = new HealpixMoc();
      t0=System.currentTimeMillis();
      moc1.read(filename);
      t1=System.currentTimeMillis();
      System.out.println(".insertion from JSON file (no check): "+(t1-t0)+"ms");
      (new File(filename)).delete();
      if( !moc.equals(moc1) ) {
         System.out.println("MocTest.testIterativeInsertion ERROR: inconsistency results E:\n.moc::"+moc.todebug()+"\n.moc1:"+moc1.todebug());
         return false;
      }

      
      
//      System.out.println("Moc result:"+moc.todebug());
            
      System.out.println("testIterativeInsertion OK");
      return true;
   }




   private static boolean testHierarchy() throws Exception {
      title("testHierarchy: Create a Moc manually and check isIn(), isAscendant() and isDescendant() methods...");
      String ref = "3/10-12 5/128";
      HealpixMoc moc = new HealpixMoc(ref);
      boolean b;
      boolean rep=true;
      System.out.println("REF: "+ref);
      System.out.println("MOC:\n"+moc);
      System.out.println("- 3/11 [asserting true] isIn()="+(b=moc.isIn(3,11))); rep &= b;
      System.out.println("- 3/12 [asserting true] isIn()="+(b=moc.isIn(3,11))); rep &= b;
      System.out.println("- 2/0 [asserting false] isAscendant()="+(b=moc.isAscendant(2,0))); rep &= !b;
      System.out.println("- 1/0 [asserting true] isAscendant()="+(b=moc.isAscendant(1,0))); rep &= b;
      System.out.println("- 6/340000 [asserting false] isDescendant()="+(b=moc.isDescendant(6,340000)));  rep &=!b;
      System.out.println("- 6/515 [asserting true] isDescendant()="+(b=moc.isDescendant(6,514))); rep &=b;
      if( !rep ) System.out.println("MocTest.testContains ERROR:");
      else System.out.println("testContains OK");
      return rep;
   }

   private static boolean testContains() throws Exception {
      title("testContains: Create a Moc manually and check contains() methods...");
      Healpix hpx = new Healpix();
      HealpixMoc moc = new HealpixMoc("2/0 3/10 4/35");
      System.out.println("MOC: "+moc);
      boolean rep=true;
      try {
         System.out.println("- contains(028.93342,+18.18931) [asserting IN]    => "+moc.contains(hpx,028.93342,18.18931)); rep &= moc.contains(hpx,028.93342,18.18931);
         System.out.println("- contains(057.23564,+15.34922) [asserting OUT]   => "+moc.contains(hpx,057.23564,15.34922)); rep &= !moc.contains(hpx,057.23564,15.34922);
         System.out.println("- contains(031.89266,+17.07820) [asserting IN]    => "+moc.contains(hpx,031.89266,17.07820)); rep &= moc.contains(hpx,031.89266,17.07820);
      } catch( Exception e ) {
         e.printStackTrace();
         rep=false;
      }
      if( !rep ) System.out.println("MocTest.testContains ERROR:");
      else System.out.println("MocTest.testContains OK");
      return rep;
  }

   private static boolean testFITS() throws Exception {
      title("testFITS: Create a MOC manually, write it in FITS and re-read it...");
      HealpixMoc moc = new HealpixMoc();
      moc.add("3/10 4/12-15 18 22");
      moc.add("4/13-18 5/19 20");
      moc.add("17/222 28/123456789");
      String mocS="{ \"3\":[3,10], \"4\":[16,17,18,22], \"5\":[19,20], \"17\":[222], \"28\":[123456789] }";
      int mocOrder = 28;
     
      String file = "/Users/Pierre/Desktop/__MOC.fits";
      System.out.println("- MOC created: "+moc);
      moc.write(file,HealpixMoc.FITS);
      System.out.println("- test write (FITS) seems OK");
      
      StringBuilder trace = new StringBuilder();
      FileInputStream in = new FileInputStream(file);
      int rep = MocLint.check(trace,in);
      in.close();
      if( rep==1 ) System.out.println("- test read (FITS) OK and IVOA valid");
      else if( rep==-1 ) System.out.println("- test read (FITS) WARNING, MOC ok but IVOA unvalid");
      if( rep!=1 ) System.out.println(trace);
      if( rep==0 ) {
         System.out.println("MocTest.testFITS ERROR: not IVOA valid");
         return false;
      }
      
      moc = new HealpixMoc();
      moc.read(file);
      System.out.println("- MOC re-read: "+moc);
      if( !moc.toString().equals(mocS) ) {
         System.out.println("MocTest.testFITS ERROR: waiting=["+mocS+"]");
         return false;
      }
      if( moc.getMocOrder()!=mocOrder ) {
         System.out.println("MocTest.testJSON ERROR: wrong mocOrder "+moc.getMocOrder()+" waiting "+mocOrder);
         return false;
      }
      
      System.out.println("testFITS OK");
      return true;
   }
   
   private static boolean testJSON() throws Exception {
      title("testJSON: Create a MOC manually, write it in JSON and re-read it...");
      HealpixMoc moc = new HealpixMoc();
      moc.add("3/10 4/12-15 18 22");
      moc.add("4/13-18 5/19 20");
      moc.add("17/222 28/123456789");
      String mocS="{ \"3\":[3,10], \"4\":[16,17,18,22], \"5\":[19,20], \"17\":[222], \"28\":[123456789] }";
      int mocOrder = 28;
      
      String file = "/Users/Pierre/Desktop/__MOC.json";
      System.out.println("- MOC created: "+moc);
      moc.write(file,HealpixMoc.JSON);
      System.out.println("- test write (JSON) seems OK");
      
      moc = new HealpixMoc();
      moc.read(file);
      System.out.println("- MOC re-read: "+moc);
      if( !moc.toString().equals(mocS) ) {
         System.out.println("MocTest.testJSON ERROR: waiting=["+mocS+"]");
         return false;
      }
      if( moc.getMocOrder()!=mocOrder ) {
         System.out.println("MocTest.testJSON ERROR: wrong mocOrder "+moc.getMocOrder()+" waiting "+mocOrder);
         return false;
      }
      
      System.out.println("testJSON OK");
      return true;
   }
   
   private static boolean testJSON0() throws Exception {
      title("testJSON0: read old JSON format (with #MOCORDER commment) for ascending compatibility...");
      String s =
         "#MOCORDER 28\n" +
         "{\"3\":[3,10],\n" +
         "\"4\":[16,17,18,22],\n" +
         "\"5\":[19,20],\n" +
         "\"17\":[222],\n" +
         "\"28\":[123456789]}\n";
      int mocOrder = 28;
      InputStream stream = new ByteArrayInputStream(s.getBytes());
      
      String mocS="{ \"3\":[3,10], \"4\":[16,17,18,22], \"5\":[19,20], \"17\":[222], \"28\":[123456789] }";
      
      HealpixMoc moc = new HealpixMoc();
      moc.read(stream);
      System.out.println("- MOC read: "+moc);
      if( !moc.toString().equals(mocS) ) {
         System.out.println("MocTest.testJSON0 ERROR: waiting=["+mocS+"]");
         return false;
      }
      if( moc.getMocOrder()!=mocOrder ) {
         System.out.println("MocTest.testJSON0 ERROR: wrong mocOrder "+moc.getMocOrder()+" waiting "+mocOrder);
         return false;
      }
      
      System.out.println("testJSON0 OK");
      return true;
   }
   
   private static boolean testASCII0() throws Exception {
      title("testASCII0: read ASCII0 format (with #MOCORDER commment) for ascending compatibility...");
      String s =
            "#MOCORDER 28\n" +
            "3/3,10 4/16,17,18,22 5/19,20\n" +
            "17/222 28/123456789\n";
      int mocOrder = 28;
      InputStream stream = new ByteArrayInputStream(s.getBytes());
      
      String mocS="{ \"3\":[3,10], \"4\":[16,17,18,22], \"5\":[19,20], \"17\":[222], \"28\":[123456789] }";
      
      HealpixMoc moc = new HealpixMoc();
      moc.read(stream);
      System.out.println("- MOC read: "+moc);
      if( !moc.toString().equals(mocS) ) {
         System.out.println("MocTest.testASCII0 ERROR: waiting=["+mocS+"]");
         return false;
      }
      if( moc.getMocOrder()!=mocOrder ) {
         System.out.println("MocTest.testASCII0 ERROR: wrong mocOrder "+moc.getMocOrder()+" waiting "+mocOrder);
         return false;
      }
      
      System.out.println("testASCII0 OK");
      return true;
   }
   
   private static boolean testASCII() throws Exception {
      title("testASCII: read ASCII format...");
      String s =
            "3/3,10 4/16,17,18,22 5/19,20\n" +
            "17/222 28/123456789\n";
      int mocOrder = 28;
      InputStream stream = new ByteArrayInputStream(s.getBytes());
      
      String mocS="{ \"3\":[3,10], \"4\":[16,17,18,22], \"5\":[19,20], \"17\":[222], \"28\":[123456789] }";
      
      HealpixMoc moc = new HealpixMoc();
      moc.read(stream);
      System.out.println("- MOC read: "+moc);
      if( !moc.toString().equals(mocS) ) {
         System.out.println("MocTest.testASCII ERROR: waiting=["+mocS+"]");
         return false;
      }
      if( moc.getMocOrder()!=mocOrder ) {
         System.out.println("MocTest.testASCII ERROR: wrong mocOrder "+moc.getMocOrder()+" waiting "+mocOrder);
         return false;
      }
      
      System.out.println("testASCII OK");
      return true;
   }
   
   private static boolean testSTRING() throws Exception {
      title("testASCII: read STRING format...");
      String s = "3/3,10 4/16-18,22 5/19,20 17/222 28/";
      int mocOrder = 28;
      InputStream stream = new ByteArrayInputStream(s.getBytes());
      
      String mocS="{ \"3\":[3,10], \"4\":[16,17,18,22], \"5\":[19,20], \"17\":[222] }";
      
      HealpixMoc moc = new HealpixMoc();
      moc.read(stream);
      System.out.println("- MOC read: "+moc);
      if( !moc.toString().equals(mocS) ) {
         System.out.println("MocTest.testSTRING ERROR: waiting=["+mocS+"]");
         return false;
      }
      if( moc.getMocOrder()!=mocOrder ) {
         System.out.println("MocTest.testSTRING ERROR: wrong mocOrder "+moc.getMocOrder()+" waiting "+mocOrder);
         return false;
      }
      
      System.out.println("testSTRING OK");
      return true;
   }
   
   private static boolean testOperation() throws Exception {
      title("testOperation: Create 2 Mocs manually, test intersection(), union(), equals(), clone()...");
      HealpixMoc moc1 = new HealpixMoc("3/1,3-4,9 4/30-31");
      String moc1S = "{ \"3\":[1,3,4,9], \"4\":[30,31] }";
      System.out.println("- Loading moc1: "+moc1);
      if( !moc1.toString().equals(moc1S) ) {
         System.out.println("MocTest.testOperation load ERROR: waiting=["+moc1S+"]");
         return false;
      }
      
      HealpixMoc moc2 = new HealpixMoc("4/23 3/3 10 4/23-28;4/29 5/65");
      String moc2S = "{ \"3\":[3,6,10], \"4\":[23,28,29], \"5\":[65] }";
      System.out.println("- Loading moc2: "+moc2);
      if( !moc2.toString().equals(moc2S) ) {
         System.out.println("MocTest.testOperation load ERROR: waiting=["+moc2S+"]");
         return false;
      }
      
      HealpixMoc moc3 = (HealpixMoc)moc2.clone();
      System.out.println("- Cloning moc2->moc3: "+moc3);
      if( !moc3.toString().equals(moc2S) ) {
         System.out.println("MocTest.testOperation clone ERROR: waiting=["+moc2S+"]");
         return false;
      }

      HealpixMoc moc4 = moc2.intersection(moc1);
      String moc4S = "{ \"3\":[3], \"5\":[65] }";
      System.out.println("- Intersection moc2 moc1: "+moc4);
      if( !moc4.toString().equals(moc4S) ) {
         System.out.println("MocTest.testOperation intersection ERROR: waiting=["+moc4S+"]");
         return false;
      }
      if( !moc1.intersection(moc2).toString().equals(moc4S) ) {
         System.out.println("MocTest.testOperation intersection ERROR: no commutative");
         return false;
      }
     
      HealpixMoc moc5 = moc3.union(moc1);
      String moc5S = "{ \"3\":[1,3,4,6,7,9,10], \"4\":[23] }";
      System.out.println("- Union moc3 moc1: "+moc5);
      if( !moc1.union(moc2).toString().equals(moc5S) ) {
         System.out.println("MocTest.testOperation union ERROR: no commutative");
         return false;
      }
      if( !moc5.toString().equals(moc5S) ) {
         System.out.println("MocTest.testOperation union ERROR: waiting=["+moc5S+"]");
         return false;
      }

      HealpixMoc moc7 = moc1.subtraction(moc2);
      String moc7S = "{ \"3\":[1,9], \"4\":[17,18,19,30,31], \"5\":[64,66,67] }";
      System.out.println("- Subtraction moc1 - moc2: "+moc7);
      if( !moc7.toString().equals(moc7S) ) {
         System.out.println("MocTest.testOperation subtraction ERROR: waiting=["+moc7S+"]");
         return false;
      }

      String moc6S="{ \"3\":[3,6,10], \"4\":[23,28,29] }";
      HealpixMoc moc6 = new HealpixMoc(moc6S);
      boolean test=moc6.equals(moc2);
      System.out.println("- Not-equals moc2,["+moc6S+"] : "+test);
      if( test ) {
         System.out.println("MocTest.testOperation equals ERROR: waiting=[false]");
         return false;
      }
      moc6.add("5:65");
      test=moc6.equals(moc2);
      System.out.println("- Equals moc2,["+moc2S+"] : "+test);
      if( !test ) {
         System.out.println("MocTest.testOperation equals ERROR: waiting=[true]");
         return false;
      }
      
      HealpixMoc moc8 = moc1.difference(moc2);
      String moc8S = "{ \"3\":[1,6,7,9,10], \"4\":[17,18,19,23], \"5\":[64,66,67] }";
      System.out.println("- difference moc1  moc2: "+moc8);
      if( !moc8.toString().equals(moc8S) ) {
         System.out.println("MocTest.testOperation difference ERROR: waiting=["+moc8S+"]");
         return false;
      }
      if( !moc1.difference(moc2).toString().equals(moc8S) ) {
         System.out.println("MocTest.testOperation difference ERROR: no commutative");
         return false;
      }
      
//      HealpixMoc moc10=new HealpixMoc("0/2-11 1/1-3");
//      HealpixMoc moc9 = moc10.complement();
//      String moc9S = "{ \"0\":[1], \"1\":[0] }";
//      System.out.println("- Moc       : "+moc10);
//      System.out.println("- Complement: "+moc9);
//      if( !moc9.toString().equals(moc9S) ) {
//         System.out.println("MocTest.testOperation complement ERROR: waiting=["+moc9S+"]");
//         return false;
//      }

     
      System.out.println("testOperation OK");
      return true;
   }

   
   private static boolean testIsIntersecting() throws Exception {
      title("testIsInTree: Create 2 Mocs manually, and test isIntersecting() in both directions...");
      HealpixMoc moc1 = new HealpixMoc("11/25952612");
      HealpixMoc moc2 = new HealpixMoc("9/1622036,1622038");
      System.out.println("moc1="+moc1);
      System.out.println("moc2="+moc2);
      boolean rep1=moc2.isIntersecting(moc1);
      boolean rep2=moc1.isIntersecting(moc2);
      System.out.println("moc2 inter moc1 = "+rep1);
      System.out.println("moc1 inter moc2 = "+rep2);
      if( !rep1 || !rep2 ) {
         System.out.println("MocTest.isIntersecting ERROR");
         return false;
      }
      
      System.out.println("testIsInTree OK");
      return true;

   }
   
   private static boolean testRange() throws Exception {
      title("testRange: Create a Mocs manually, and test setMin and Max limitOrder()...");
      HealpixMoc moc1 = new HealpixMoc("{ \"1\":[0,1], \"2\":[8,9], \"3\":[40,53] }");
      System.out.println("moc1="+moc1);
      moc1.setCheckConsistencyFlag(false);
      moc1.add("3/37 53");
      System.out.println("moc1 + 3/37,46="+moc1);
      moc1.checkAndFix();
      System.out.println("moc1 after check="+moc1);
      String s1 = "{ \"1\":[0,1], \"2\":[8,9], \"3\":[40,53] }";
      if( !moc1.toString().equals(s1) ) {
         System.out.println("MocTest.testRange checkAndFix() ERROR: waiting=["+s1+"]");
         return false;
      }
      
      HealpixMoc moc2 = (HealpixMoc)moc1.clone();
      moc2.setMinLimitOrder(2);
      System.out.println("moc2="+moc2);
      String s2 = "{ \"2\":[0,1,2,3,4,5,6,7,8,9], \"3\":[40,53] }";
      if( !moc2.toString().equals(s2) ) {
         System.out.println("MocTest.testRange setMinLimitOrder(2) ERROR: waiting=["+s2+"]");
         return false;
      }

      HealpixMoc moc3 = (HealpixMoc)moc1.clone();
      moc3.setMaxLimitOrder(2);
      System.out.println("moc3="+moc3);
      String s3 = "{ \"1\":[0,1], \"2\":[8,9,10,13] }";
      if( !moc3.toString().equals(s3) ) {
         System.out.println("MocTest.testRange setMaxLimitOrder(2) ERROR: waiting=["+s3+"]");
         return false;
      }
      
      moc3.setMinLimitOrder(1);
      boolean in1 = moc3.isIntersecting(0, 1);
      if( in1 ) {
         System.out.println("MocTest.testRange isIntersecting(0,1) ERROR: waiting=false]");
         return false;
      }
      boolean in2 = moc3.isIntersecting(0, 0);
      if( !in2 ) {
         System.out.println("MocTest.testRange isIntersecting(0,0) ERROR: waiting=true]");
         return false;
      }
      boolean in3 = moc3.isIntersecting(3, 33);
      if( !in3 ) {
         System.out.println("MocTest.testRange isIntersecting(3,33) ERROR: waiting=true]");
         return false;
      }
      boolean in5 = moc3.isIntersecting(3, 56);
      if( in5 ) {
         System.out.println("MocTest.testRange isIntersecting(3,56) ERROR: waiting=false]");
         return false;
      }
      
      System.out.println("testRange OK");
      return true;
   }
   
   private static boolean testIterators() throws Exception {
      title("testIterators: Test on MOC iterators...");
      String ref = " 0/2 1/4 1/13";
      HealpixMoc moc = new HealpixMoc();
      moc.add(ref);
      
      // Iterator order per order
      Iterator<MocCell> it = moc.iterator();
      StringBuffer s = new StringBuffer();
      while( it.hasNext() ) {
         MocCell p = it.next();
         s.append(" "+p.order+"/"+p.npix);
      }
      boolean rep = s.toString().equals(ref);
      if( !rep ) {
         System.out.println("MocTest.testIterators [iterator()] ERROR:\n.get ["+s+"]\n.ref ["+ref+"]\n");
         return false;
      }
      
      // Iterator on low level pixel
      String ref1 = " 4 8 9 10 11 13";
      Iterator<Long> it1 = moc.pixelIterator();
      s = new StringBuffer();
      while( it1.hasNext() ) {
         Long p = it1.next();
         s.append(" "+p);
      }
      rep = s.toString().equals(ref1);
      if( !rep ) {
         System.out.println("MocTest.testIterators [pixelIterator()] ERROR:\n.get ["+s+"]\n.ref ["+ref1+"]\n");
         return false;
      }
      
      System.out.println("testIterators OK");
      return true;
   }
   
   private static boolean testInclusive() throws Exception {
      title("testInclusive: Test isIncluding()...");
      String ref = "2/1 4/33";
      HealpixMoc moc = new HealpixMoc( ref );
      System.out.println(".moc="+moc);
      
      HealpixMoc reg1 = new HealpixMoc("3/5,6");
      boolean in1 = moc.isIncluding( reg1 );
      System.out.println(".reg1="+reg1+" is included ? => "+in1);
      if( !in1 ) {
         System.out.println("MocTest.testInclusive ERROR: should be true]");
         return false;
      }
      HealpixMoc reg2 = new HealpixMoc("3/5,8");
      boolean in2 = moc.isIncluding( reg2 );
      System.out.println(".reg2="+reg2+" is included ? => "+in2);
      if( in2 ) {
         System.out.println("MocTest.testInclusive ERROR: should be false]");
         return false;
      }
      
      HealpixMoc reg3 = new HealpixMoc("4/33");
      boolean in3 = moc.isIncluding( reg3 );
      System.out.println(".reg3="+reg3+" is included ? => "+in3);
      if( !in3 ) {
         System.out.println("MocTest.testInclusive ERROR: should be true]");
         return false;
      }
      
      HealpixMoc reg4 = new HealpixMoc("4/34");
      boolean in4 = moc.isIncluding( reg4 );
      System.out.println(".reg4="+reg4+" is included ? => "+in4);
      if( in4 ) {
         System.out.println("MocTest.testInclusive ERROR: should be false]");
         return false;
      }
      
      System.out.println("testInclusive OK");
      return true;

   }
   
   private static void title(String s) {
      StringBuffer s1 = new StringBuffer(100);
      s1.append('\n');
      for( int i=0; i<20; i++ ) s1.append('-');
      s1.append(" "+s+" ");
      for( int i=0; i<20; i++ ) s1.append('-');
      System.out.println(s1);
   }
   
   class Source {
      double ra,de,rad;
      Source(double ra,double de,double rad) {
         this.ra=ra; this.de=de; this.rad=rad;
      }
   }
   

// Juste pour tester
   public static void main(String[] args) {
      boolean ok=true;
      
      try {
//         if( true ) System.exit(0);
         
         ok&=testFITS();
         ok&=testJSON0();
         ok&=testJSON();
         ok&=testASCII0();
         ok&=testASCII();
         ok&=testSTRING();
         
         ok&=testBasic();
         ok&=testIterators();
         ok&=testSetLimitOrder();
         
         ok&=testContains(); 
         ok&=testOperation(); 
         
         ok&=testHierarchy();
         ok&=testRange();
         ok&=testIsIntersecting();
         ok&=testInclusive();
         ok&=testIterativeInsertion();
         
         if( ok ) System.out.println("-------------- All is fine  -----------");
         else System.out.println("-------------- There is a problem  -----------");
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }



}
