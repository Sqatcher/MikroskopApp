package com.example.microscopeapp.Frames;

public class Frames
{
    private static final int FRAME_WIDTH  = 640;
    private static final int FRAME_HEIGHT = 480;

    static public native int   open_shm(String shm_name);
    static public native byte[] get_frame();

    public Frames()
    {
        System.loadLibrary("frames");
    }

//  public static void main(String[] args)
//   {
    //  }
}





















