package imohoo.com.mycamera.unitl;

/**
 * Created by xcs2 on 2017/1/19.
 */

public enum InterfaceOrientation {
    Portrait(0, 0, 0, false, 315, 45),
    LandscapeRight(1, 90, 1, true, 45, 135),
    PortraitUpsideDown(2, 180, 2, false, 135, 225),
    LandscapeLeft(3, 270, 3, true, 225, 315);
    private int a;
    private int b;
    private int c;
    private boolean d;
    private int e;
    private int f;
    private InterfaceOrientation(int var3, int var4, int var5, boolean var6, int var7, int var8) {
        this.a = var3;
        this.c = var4;
        this.b = var5;
        this.d = var6;
        this.e = var7;
        this.f = var8;
    }
    public final int getFlag() {
        return this.a;
    }

    public final int getSurfaceRotation() {
        return this.b;
    }

    public final int getDegree() {
        return this.c;
    }

    public final boolean isTransposed() {
        return this.d;
    }

    public final int viewDegree() {
        int var1;
        if((var1 = 360 - this.c) == 360) {
            var1 = 0;
        }

        return var1;
    }

    public final int[] viewFromToDegree(int var1) {
        int[] var2;
        if((var2 = new int[]{var1, this.viewDegree()})[0] == 270 && var2[1] == 0) {
            var2[1] = 360;
        } else if(var2[0] == 0 && var2[1] == 270) {
            var2[0] = 360;
        }

        return var2;
    }

    public final boolean isMatch(int var1) {
        if(this.c > 0) {
            if(var1 >= this.e && var1 < this.f) {
                return true;
            }
        } else if(var1 >= this.e || var1 < this.f) {
            return true;
        }

        return false;
    }

    public static InterfaceOrientation getWithSurfaceRotation(int var0) {
        InterfaceOrientation[] var4;
        int var3 = (var4 = values()).length;

        for(int var2 = 0; var2 < var3; ++var2) {
            InterfaceOrientation var1;
            if((var1 = var4[var2]).getSurfaceRotation() == var0) {
                return var1;
            }
        }

        return Portrait;
    }

    public static InterfaceOrientation getWithDegrees(int var0) {
        if((var0 %= 360) < 0) {
            var0 += 360;
        }

        InterfaceOrientation[] var4;
        int var3 = (var4 = values()).length;

        for(int var2 = 0; var2 < var3; ++var2) {
            InterfaceOrientation var1;
            if((var1 = var4[var2]).getDegree() == var0) {
                return var1;
            }
        }

        return Portrait;
    }
}
