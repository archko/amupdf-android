package com.artifex.solib;

public class SOTransition
{
    public static final int FAST      = 500;
    public static final int VERY_SLOW = 5000;
    public static final int SLOW      = 1000;
    public static final int MEDIUM    = 750;
    public static final int VERY_FAST = 300;
    public static final int FASTEST   = 75;

    public SOTransition(String strans)
    {
        //  default fields
        mType      = "";
        mDirection = "";
        mSpeed     = "";
        mDuration = 0;

        mRawValue = strans;

        if (strans != null)
        {
            //  parse the transition string
            //  it looks like:  type:xxx; direction:xxx; speed:xxx;
            String parts[] = strans.split(";");
            for (int i=0; i<parts.length; i++)
            {
                String part = parts[i].trim();
                String x[] = part.split(":");
                if (x[0].trim().equals("type"))
                    mType = x[1].trim();
                if (x[0].trim().equals("direction"))
                    mDirection = x[1].trim();
                if (x[0].trim().equals("speed"))
                {
                    mSpeed = x[1].trim();
                    mDuration = FAST;  //  Default "fast"
                    if (mSpeed.equals("veryslow"))
                        mDuration = VERY_SLOW;
                    if (mSpeed.equals("slow"))
                        mDuration = SLOW;
                    if (mSpeed.equals("medium"))
                        mDuration = MEDIUM;
                    if (mSpeed.equals("veryfast"))
                        mDuration = VERY_FAST;
                    if (mSpeed.equals("fastest"))
                        mDuration = FASTEST;
                }
            }
        }

    }

    public String mRawValue;
    public String mType;
    public String mDirection;
    public String mSpeed;
    public int mDuration;
}
