package com.artifex.solib.animation;

public class SOAnimationEasings {
    public static final int LINEAR = 0;
    public static final int QUADRATIC_IN = 1;
    public static final int QUADRATIC_OUT = 2;
    public static final int QUADRATIC_IN_OUT = 3;
    public static final int CUBIC_IN = 4;
    public static final int CUBIC_OUT = 5;
    public static final int CUBIC_IN_OUT = 6;
    public static final int QUARTIC_IN = 7;
    public static final int QUARTIC_OUT = 8;
    public static final int QUARTIC_IN_OUT = 9;
    public static final int QUINTIC_IN = 10;
    public static final int QUINTIC_OUT = 11;
    public static final int QUINTIC_IN_OUT = 12;
    public static final int SINE_IN = 13;
    public static final int SINE_OUT = 14;
    public static final int SINE_IN_OUT = 15;
    public static final int CIRCULAR_IN = 16;
    public static final int CIRCULAR_OUT = 17;
    public static final int CIRCULAR_IN_OUT = 18;
    public static final int EXPONENTIAL_IN = 19;
    public static final int EXPONENTIAL_OUT = 20;
    public static final int EXPONENTIAL_IN_OUT = 21;
    public static final int ELASTIC_IN = 22;
    public static final int ELASTIC_OUT = 23;
    public static final int ELASTIC_IN_OUT = 24;
    public static final int BOUNCE_IN = 25;
    public static final int BOUNCE_OUT = 26;
    public static final int BOUNCE_IN_OUT = 27;
    public static final int BACK_IN = 28;
    public static final int BACK_OUT = 29;
    public static final int BACK_IN_OUT = 30;

    // Converted from AHEasing

    // Modeled after the line y = x
    public static float linear(float p) {
        return p;
    }

    // Modeled after the parabola y = x^2
    public static float quadraticEaseIn(float p) {
        return p * p;
    }

    // Modeled after the parabola y = -x^2 + 2x
    public static float quadraticEaseOut(float p) {
        return -(p * (p - 2));
    }

    // Modeled after the piecewise quadratic
    // y = (1/2)((2x)^2)             ; [0, 0.5)
    // y = -(1/2)((2x-1)*(2x-3) - 1) ; [0.5, 1]
    public static float quadraticEaseInOut(float p) {
        if (p < 0.5)
            return 2 * p * p;
        else
            return (-2 * p * p) + (4 * p) - 1;
    }

    // Modeled after the cubic y = x^3
    public static float cubicEaseIn(float p) {
        return p * p * p;
    }

    // Modeled after the cubic y = (x - 1)^3 + 1
    public static float cubicEaseOut(float p) {
        float f = (p - 1);
        return f * f * f + 1;
    }

    // Modeled after the piecewise cubic
    // y = (1/2)((2x)^3)       ; [0, 0.5)
    // y = (1/2)((2x-2)^3 + 2) ; [0.5, 1]
    public static float cubicEaseInOut(float p) {
        if(p < 0.5) {
            return 4 * p * p * p;
        } else {
            float f = ((2 * p) - 2);
            return 0.5f * f * f * f + 1;
        }
    }

    // Modeled after the quartic x^4
    public static float quarticEaseIn(float p) {
        return p * p * p * p;
    }

    // Modeled after the quartic y = 1 - (x - 1)^4
    public static float quarticEaseOut(float p) {
        float f = (p - 1);
        return f * f * f * (1 - p) + 1;
    }

    // Modeled after the piecewise quartic
    // y = (1/2)((2x)^4)        ; [0, 0.5)
    // y = -(1/2)((2x-2)^4 - 2) ; [0.5, 1]
    public static float quarticEaseInOut(float p) {
        if(p < 0.5) {
            return 8 * p * p * p * p;
        } else {
            float f = (p - 1);
            return -8 * f * f * f * f + 1;
        }
    }

    // Modeled after the quintic y = x^5
    public static float quinticEaseIn(float p) {
        return p * p * p * p * p;
    }

    // Modeled after the quintic y = (x - 1)^5 + 1
    public static float quinticEaseOut(float p) {
        float f = (p - 1);
        return f * f * f * f * f + 1;
    }

    // Modeled after the piecewise quintic
    // y = (1/2)((2x)^5)       ; [0, 0.5)
    // y = (1/2)((2x-2)^5 + 2) ; [0.5, 1]
    public static float quinticEaseInOut(float p) {
        if(p < 0.5) {
            return 16 * p * p * p * p * p;
        } else {
            float f = ((2 * p) - 2);
            return 0.5f * f * f * f * f * f + 1;
        }
    }

    // Modeled after quarter-cycle of sine wave
    public static float sineEaseIn(float p) {
        return (float) Math.sin((p - 1) * Math.PI / 2.0) + 1;
    }

    // Modeled after quarter-cycle of sine wave (different phase)
    public static float sineEaseOut(float p) {
        return (float) Math.sin(p * Math.PI / 2.0);
    }

    // Modeled after half sine wave
    public static float sineEaseInOut(float p) {
        return 0.5f * (float) (1 - Math.cos(p * Math.PI));
    }

    // Modeled after shifted quadrant IV of unit circle
    public static float circularEaseIn(float p) {
        return 1 - (float) Math.sqrt(1 - (p * p));
    }

    // Modeled after shifted quadrant II of unit circle
    public static float circularEaseOut(float p) {
        return (float) Math.sqrt((2 - p) * p);
    }

    // Modeled after the piecewise circular function
    // y = (1/2)(1 - sqrt(1 - 4x^2))           ; [0, 0.5)
    // y = (1/2)(sqrt(-(2x - 3)*(2x - 1)) + 1) ; [0.5, 1]
    public static float circularEaseInOut(float p) {
        if(p < 0.5)
            return 0.5f * (1 - (float) Math.sqrt(1 - 4 * (p * p)));
        else
            return 0.5f * ((float) Math.sqrt(-((2 * p) - 3) * ((2 * p) - 1)) + 1);
    }

    // Modeled after the exponential function y = 2^(10(x - 1))
    public static float exponentialEaseIn(float p) {
        return (p == 0.0) ? p : (float) Math.pow(2, 10 * (p - 1));
    }

    // Modeled after the exponential function y = -2^(-10x) + 1
    public static float exponentialEaseOut(float p) {
        return (p == 1.0) ? p : 1 - (float) Math.pow(2, -10 * p);
    }

    // Modeled after the piecewise exponential
    // y = (1/2)2^(10(2x - 1))         ; [0,0.5)
    // y = -(1/2)*2^(-10(2x - 1))) + 1 ; [0.5,1]
    public static float exponentialEaseInOut(float p) {
        if(p == 0.0 || p == 1.0)
            return p;
        if(p < 0.5)
            return 0.5f * (float) Math.pow(2, (20 * p) - 10);
        else
            return -0.5f * (float) Math.pow(2, (-20 * p) + 10) + 1;
    }

    // Modeled after the damped sine wave y = sin(13pi/2*x)*pow(2, 10 * (x - 1))
    public static float elasticEaseIn(float p) {
        return (float) (Math.sin(13 * Math.PI / 2.0 * p) * Math.pow(2, 10 * (p - 1)));
    }

    // Modeled after the damped sine wave y = sin(-13pi/2*(x + 1))*pow(2, -10x) + 1
    public static float elasticEaseOut(float p) {
        return (float) (Math.sin(-13 * Math.PI / 2.0 * (p + 1)) * Math.pow(2, -10 * p) + 1);
    }

    // Modeled after the piecewise exponentially-damped sine wave:
    // y = (1/2)*sin(13pi/2*(2*x))*pow(2, 10 * ((2*x) - 1))      ; [0,0.5)
    // y = (1/2)*(sin(-13pi/2*((2x-1)+1))*pow(2,-10(2*x-1)) + 2) ; [0.5, 1]
    public static float elasticEaseInOut(float p) {
        if(p < 0.5)
            return 0.5f * (float) (Math.sin(13 * Math.PI / 2.0 * (2 * p)) * Math.pow(2, 10 * ((2 * p) - 1)));
        else
            return 0.5f * (float) (Math.sin(-13 * Math.PI / 2.0 * ((2 * p - 1) + 1)) * Math.pow(2, -10 * (2 * p - 1)) + 2);
    }

    // Modeled after the overshooting cubic y = x^3-x*sin(x*pi)
    public static float backEaseIn(float p) {
        return p * p * p - p * (float) Math.sin(p * Math.PI);
    }

    // Modeled after overshooting cubic y = 1-((1-x)^3-(1-x)*sin((1-x)*pi))
    public static float backEaseOut(float p) {
        float f = (1 - p);
        return 1 - (f * f * f - f * (float) Math.sin(f * Math.PI));
    }

    // Modeled after the piecewise overshooting cubic function:
    // y = (1/2)*((2x)^3-(2x)*sin(2*x*pi))           ; [0, 0.5)
    // y = (1/2)*(1-((1-x)^3-(1-x)*sin((1-x)*pi))+1) ; [0.5, 1]
    public static float backEaseInOut(float p) {
        if(p < 0.5) {
            float f = 2 * p;
            return 0.5f * (f * f * f - f * (float) Math.sin(f * Math.PI));
        } else {
            float f = (1 - (2*p - 1));
            return 0.5f * (1 - (f * f * f - f * (float) Math.sin(f * Math.PI))) + 0.5f;
        }
    }

    public static float bounceEaseIn(float p) {
        return 1 - SOAnimationEasings.bounceEaseOut(1 - p);
    }

    public static float bounceEaseOut(float p) {
        if(p < 4/11.0) {
            return (121 * p * p)/16.0f;
        } else if(p < 8/11.0) {
            return (363/40.0f * p * p) - (99/10.0f * p) + 17/5.0f;
        } else if(p < 9/10.0) {
            return (4356/361.0f * p * p) - (35442/1805.0f * p) + 16061/1805.0f;
        } else {
            return (54/5.0f * p * p) - (513/25.0f * p) + 268/25.0f;
        }
    }

    public static float bounceEaseInOut(float p) {
        if(p < 0.5) {
            return 0.5f * bounceEaseIn(p*2);
        } else {
            return 0.5f * bounceEaseOut(p * 2 - 1) + 0.5f;
        }
    }

    public static float ease(int easing, float x, float d, float t)
    {
        switch (easing) {
            default:
            case LINEAR:             t = linear(t);               break;
            case QUADRATIC_IN:       t = quadraticEaseIn(t);      break;
            case QUADRATIC_OUT:      t = quadraticEaseOut(t);     break;
            case QUADRATIC_IN_OUT:   t = quadraticEaseInOut(t);   break;
            case CUBIC_IN:           t = cubicEaseIn(t);          break;
            case CUBIC_OUT:          t = cubicEaseOut(t);         break;
            case CUBIC_IN_OUT:       t = cubicEaseInOut(t);       break;
            case QUARTIC_IN:         t = quarticEaseIn(t);        break;
            case QUARTIC_OUT:        t = quarticEaseOut(t);       break;
            case QUARTIC_IN_OUT:     t = quarticEaseInOut(t);     break;
            case QUINTIC_IN:         t = quinticEaseIn(t);        break;
            case QUINTIC_OUT:        t = quinticEaseOut(t);       break;
            case QUINTIC_IN_OUT:     t = quinticEaseInOut(t);     break;
            case SINE_IN:            t = sineEaseIn(t);           break;
            case SINE_OUT:           t = sineEaseOut(t);          break;
            case SINE_IN_OUT:        t = sineEaseInOut(t);        break;
            case CIRCULAR_IN:        t = circularEaseIn(t);       break;
            case CIRCULAR_OUT:       t = circularEaseOut(t);      break;
            case CIRCULAR_IN_OUT:    t = circularEaseInOut(t);    break;
            case EXPONENTIAL_IN:     t = exponentialEaseIn(t);    break;
            case EXPONENTIAL_OUT:    t = exponentialEaseOut(t);   break;
            case EXPONENTIAL_IN_OUT: t = exponentialEaseInOut(t); break;
            case ELASTIC_IN:         t = elasticEaseIn(t);        break;
            case ELASTIC_OUT:        t = elasticEaseOut(t);       break;
            case ELASTIC_IN_OUT:     t = elasticEaseInOut(t);     break;
            case BOUNCE_IN:          t = bounceEaseIn(t);         break;
            case BOUNCE_OUT:         t = bounceEaseOut(t);        break;
            case BOUNCE_IN_OUT:      t = bounceEaseInOut(t);      break;
            case BACK_IN:            t = backEaseIn(t);           break;
            case BACK_OUT:           t = backEaseOut(t);          break;
            case BACK_IN_OUT:        t = backEaseInOut(t);        break;
        }

        return x + t * d;
    }
}
