package nickrout.lenslauncher.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.util.Log;

import nickrout.lenslauncher.model.Grid;

/**
 * Created by nickrout on 2016/04/02.
 */
public class LensCalculator {

    // Algorithm for calculating equispaced grid
    public static Grid calculateGrid(Context context, int screenWidth, int screenHeight, int itemCount, int desiredOffset) {
        Grid grid = new Grid();
        grid.setItemCount(itemCount);
        double multiplier = Math.sqrt((double) itemCount);
        int itemCountHorizontal = (int) Math.ceil(multiplier * ((double) screenWidth / (double) screenHeight));
        grid.setItemCountHorizontal(itemCountHorizontal);
        int itemCountVertical = (int) Math.ceil(multiplier * ((double) screenHeight / (double) screenWidth));
        grid.setItemCountVertical(itemCountVertical);
        Settings settings = new Settings(context);
        float itemSize = LensCalculator.convertDpToPixel(settings.getFloat(Settings.KEY_MIN_ICON_SIZE), context);
        grid.setItemSize(itemSize);
        float spacingHorizontal = (((float) screenWidth) - ((float) itemCountHorizontal * itemSize)) / ((float) (itemCountHorizontal + 1));
        grid.setSpacingHorizontal(spacingHorizontal);
        float spacingVertical = (((float) screenHeight) - ((float) itemCountVertical * itemSize)) / ((float) (itemCountVertical + 1));
        int currentOffset = (int) (((float) screenHeight - ((float) itemCountVertical * (itemSize + spacingVertical))) / 2.0f);
        if (currentOffset < desiredOffset) {
            float newSpacingVertical = (((float) screenHeight - 2.0f * (float) desiredOffset) / (float) itemCountVertical) - itemSize;
            grid.setSpacingVertical(newSpacingVertical);
        } else {
            grid.setSpacingVertical(spacingVertical);
        }
        return grid;
    }

    // Algorithm for circular distance
    public static double calculateDistance(float x1, float x2, float y1, float y2) {
        return Math.sqrt(Math.pow((double)(x2 - x1), 2) + Math.pow((double)(y2 - y1), 2));
    }

    // Algorithm for determining whether a rect is within a given lens (centered at touchX, touchY)
    public static boolean isRectWithinLens(RectF rect, float touchX, float touchY, float lensDiameter) {
        if (rect.left >= touchX - lensDiameter / 2.0f &&
            rect.right <= touchX + lensDiameter / 2.0f &&
            rect.top >= touchY - lensDiameter / 2.0f &&
            rect.bottom <= touchY + lensDiameter / 2.0f) {
            return true;
        } else {
            return false;
        }
    }

    // Graphical Fisheye Lens algorithm for shifting
    public static float shiftPoint(Context context, float lensPosition, float itemPosition, float boundary) {
        if(lensPosition < 0) {
            return itemPosition;
        }
        Settings settings =  new Settings(context);
        float shiftedPosition = itemPosition;
        float a = Math.abs(lensPosition - itemPosition);
        float x = a / (boundary / 2.0f);
        float y = ((1.0f + settings.getFloat(Settings.KEY_DISTORTION_FACTOR)) * x) / (1.0f + (settings.getFloat(Settings.KEY_DISTORTION_FACTOR) * x));
        float newDistanceFromCenter = (boundary / 2.0f) * y;
        // Removed - handled in LensView and causes weird edge 'ballooning'
        //if ((lensPosition + boundary / 2.0f) >= itemPosition && (lensPosition - boundary / 2) <= itemPosition) {
            if (lensPosition > itemPosition) {
                shiftedPosition = lensPosition - newDistanceFromCenter;
            } else if (lensPosition < itemPosition) {
                shiftedPosition = lensPosition + newDistanceFromCenter;
            }
        //}
        return shiftedPosition;
    }

    // Graphical Fisheye Lens algorithm for scaling
    public static float scalePoint(Context context, float lensPosition, float itemPosition, float itemSize, float boundary) {
        if(lensPosition < 0) {
            return itemSize;
        }
        Settings settings =  new Settings(context);
        float scaledPosition = itemPosition;
        if (lensPosition > itemPosition) {
            itemPosition = itemPosition - settings.getFloat(Settings.KEY_SCALE_FACTOR) * (itemSize / 2.0f);
        } else {
            itemPosition = itemPosition + settings.getFloat(Settings.KEY_SCALE_FACTOR) * (itemSize / 2.0f);
        }
        float a = Math.abs(lensPosition - itemPosition);
        float x = a / (boundary / 2.0f);
        float y = ((1.0f + settings.getFloat(Settings.KEY_DISTORTION_FACTOR)) * x) / (1.0f + (settings.getFloat(Settings.KEY_DISTORTION_FACTOR) * x));
        float scaledDistanceFromCenter = (boundary / 2.0f) * y;
        // Removed - handled in LensView and causes weird edge 'ballooning'
        //if ((lensPosition + boundary / 2.0f) >= itemPosition && (lensPosition - boundary / 2) <= itemPosition) {
            if (lensPosition > itemPosition) {
                scaledPosition = lensPosition - scaledDistanceFromCenter;
            } else if (lensPosition < itemPosition) {
                scaledPosition = lensPosition + scaledDistanceFromCenter;
            }
        //}
        return scaledPosition;
    }

    // Graphical Fisheye Lens algorithm for determining final scaled size
    public static float calculateSquareScaledSize(float scaledPositionX, float shiftedPositionX, float scaledPositionY, float shiftedPositionY) {
        return 2.0f * Math.min(Math.abs(scaledPositionX - shiftedPositionX), Math.abs(scaledPositionY - shiftedPositionY));
    }

    // Algorithm for calculating new rect
    public static RectF calculateRect(float newCenterX, float newCenterY, float newSize) {
        RectF newRect = new RectF(newCenterX - newSize / 2.0f,
                newCenterY - newSize / 2.0f,
                newCenterX + newSize / 2.0f,
                newCenterY + newSize / 2.0f);
        return newRect;
    }

    // Algorithm for determining if touch point is within rect
    public static boolean isInsideRect(float x, float y, RectF rect) {
        if (x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom) {
            return true;
        } else {
            return false;
        }
    }

    // Algorithm for determining grid offset in order to center vertically
    public static float calculateGridOffset(Grid grid, float height) {
        if (grid.getItemCount() > 0) {
            float offsetTop = grid.getSpacingVertical();
            double actualItemCountVertical = Math.ceil((double) (((float) grid.getItemCount()) / ((float) grid.getItemCountHorizontal())));
            float offsetBottom = height - (((float) actualItemCountVertical) * (grid.getSpacingVertical() + grid.getItemSize()));
            float offsetAverage = (offsetTop + offsetBottom) / 2.0f;
            return offsetAverage - offsetTop;
        }
        return 0.0f;
    }

    // Algorithm for converting dp measurements to pixels
    public static float convertDpToPixel(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }

    // Algorithm for converting pixels to dp measurements
    public static float convertPixelsToDp(float px, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return dp;
    }
}
