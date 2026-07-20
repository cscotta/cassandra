public class Busy { public static void main(String[] a) throws Exception {
    long x=0; while(true){ for(int i=0;i<2_000_000;i++) x+=i*31L+(x>>1); if(x==-1) Thread.sleep(1);} } }
