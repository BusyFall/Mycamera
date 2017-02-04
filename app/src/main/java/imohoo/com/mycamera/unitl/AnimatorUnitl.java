package imohoo.com.mycamera.unitl;

import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.view.View;

/**
 * Created by xcs2 on 2017/1/19.
 */

public class AnimatorUnitl {
    public static ViewPropertyAnimatorCompat rotateAnimation(View view, InterfaceOrientation interfaceOrientation, int time) {
        if (view != null && interfaceOrientation != null) {
            int[] rotation = interfaceOrientation.viewFromToDegree((int) ViewCompat.getRotation(view));
            ViewCompat.setRotation(view, (float) rotation[0]);
            final int degree = interfaceOrientation.viewDegree();
            return ViewCompat.animate(view).rotation((float) rotation[1]).setDuration((long) time).setListener(new ViewPropertyAnimatorListenerAdapter() {
                public void onAnimationEnd(View var1) {
                    ViewCompat.setRotation(var1, (float) degree);
                }
            });
        } else {
            return null;
        }
    }
}
