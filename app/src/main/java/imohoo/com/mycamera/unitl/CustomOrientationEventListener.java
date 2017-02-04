package imohoo.com.mycamera.unitl;

import android.content.Context;

/**
 * Created by xcs2 on 2017/1/19.
 */

public class CustomOrientationEventListener extends OrientationEventListener {
    private int angle;
    //取向度委托
    private CustomOrientationDegreeDelegate mCustomOrientationDegreeDelegate;
    //定位委托
    private CustomOrientationDelegate mCustomOrientationDelegate;
    private InterfaceOrientation mInterfaceOrientation;
    private boolean enable;
    /**
     * 获得设备角度
     *
     * @return
     */
    public int getDeviceAngle() {
        return this.angle;
    }

    public void setDelegate(CustomOrientationDelegate var1, CustomOrientationDegreeDelegate var2) {
        this.mCustomOrientationDelegate = var1;
        this.mCustomOrientationDegreeDelegate = var2;
    }
    public CustomOrientationEventListener(Context context) {
        super(context);
    }

    @Override
    public void onOrientationChanged(int var1) {
        this.angle = var1;
        if (this.mCustomOrientationDegreeDelegate != null) {
            this.mCustomOrientationDegreeDelegate.onOrientationDegreeChanged(var1);
        }

        int var2 = var1;
        InterfaceOrientation var3;
        InterfaceOrientation var4 = var3 = this.getOrien();
        InterfaceOrientation[] var8;
        int var7 = (var8 = InterfaceOrientation.values()).length;

        for (int var6 = 0; var6 < var7; ++var6) {
            InterfaceOrientation var5;
            if ((var5 = var8[var6]).isMatch(var2)) {
                var4 = var5;
                break;
            }
        }

        this.mInterfaceOrientation = var4;
        if ((this.enable || var4 != var3) && this.mCustomOrientationDelegate != null) {
            this.mCustomOrientationDelegate.onOrientationChanged(var4);
            this.enable = false;
        }

    }
    public InterfaceOrientation getOrien() {
        if(this.mInterfaceOrientation == null) {
            this.mInterfaceOrientation = InterfaceOrientation.Portrait;
        }

        return this.mInterfaceOrientation;
    }

    public interface CustomOrientationDegreeDelegate {
        void onOrientationDegreeChanged(int var1);
    }

    public interface CustomOrientationDelegate {
        void onOrientationChanged(InterfaceOrientation var1);
    }
}
