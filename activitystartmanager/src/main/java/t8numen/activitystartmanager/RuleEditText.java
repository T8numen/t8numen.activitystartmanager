package t8numen.activitystartmanager;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Layout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.EditText;

public class RuleEditText extends EditText {
    private static final float MIN_TEXT_SIZE_SP = 9f;
    private static final float MAX_TEXT_SIZE_SP = 16f;
    private static final float TEXT_SIZE_STEP_SP = 1f;

    private final Paint lineNumberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect lineBounds = new Rect();
    private ScaleGestureDetector scaleDetector;
    private boolean allowLongPressSelection = true;
    private boolean textGestureActive = true;
    private int gutterWidth;
    private int originalPaddingTop;
    private int originalPaddingRight;
    private int originalPaddingBottom;

    public RuleEditText(Context context) {
        super(context);
        init();
    }

    public RuleEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RuleEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gutterWidth = dp(44);
        originalPaddingTop = getPaddingTop();
        originalPaddingRight = getPaddingRight();
        originalPaddingBottom = getPaddingBottom();
        lineNumberPaint.setColor(Color.rgb(130, 130, 130));
        lineNumberPaint.setTextAlign(Paint.Align.RIGHT);
        lineNumberPaint.setTextSize(getTextSize() * 0.9f);
        dividerPaint.setColor(Color.rgb(210, 210, 210));
        setHorizontallyScrolling(true);
        setPadding(gutterWidth + dp(8), originalPaddingTop, originalPaddingRight, originalPaddingBottom);
        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleText(detector.getScaleFactor());
                return true;
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Layout layout = getLayout();
        CharSequence text = getText();
        if (layout != null && !TextUtils.isEmpty(text)) {
            int lineCount = layout.getLineCount();
            int visibleStart = getScrollY();
            int visibleEnd = visibleStart + getHeight();
            int numberRight = getScrollX() + gutterWidth - dp(8);
            int dividerX = getScrollX() + gutterWidth;
            canvas.drawLine(dividerX, visibleStart, dividerX, visibleEnd, dividerPaint);
            for (int line = 0; line < lineCount; line++) {
                int baseline = getLineBounds(line, lineBounds);
                if (lineBounds.bottom < visibleStart || lineBounds.top > visibleEnd) {
                    continue;
                }
                int start = layout.getLineStart(line);
                int end = layout.getLineEnd(line);
                if (isBlankLine(text, start, end)) {
                    continue;
                }
                canvas.drawText(String.valueOf(line + 1), numberRight, baseline, lineNumberPaint);
            }
        }
        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            textGestureActive = isTouchOnText(event);
            allowLongPressSelection = textGestureActive;
            setLongClickable(allowLongPressSelection);
            if (!textGestureActive) {
                return false;
            }
        }

        if (!textGestureActive) {
            resetTextGestureIfFinished(event);
            return false;
        }

        scaleDetector.onTouchEvent(event);
        boolean gestureFinished = event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL;
        if (event.getPointerCount() > 1) {
            resetTextGestureIfFinished(event);
            return true;
        }
        boolean handled = super.onTouchEvent(event);
        if (gestureFinished) {
            resetTextGestureState();
        }
        return handled;
    }

    @Override
    public boolean performLongClick() {
        if (!allowLongPressSelection) {
            return false;
        }
        return super.performLongClick();
    }

    public void zoomIn() {
        setTextSizeSp(getCurrentTextSizeSp() + TEXT_SIZE_STEP_SP);
    }

    public void zoomOut() {
        setTextSizeSp(getCurrentTextSizeSp() - TEXT_SIZE_STEP_SP);
    }

    public void scaleText(float scaleFactor) {
        setTextSizeSp(getCurrentTextSizeSp() * scaleFactor);
    }

    private void setTextSizeSp(float textSizeSp) {
        float safeSize = Math.max(MIN_TEXT_SIZE_SP, Math.min(MAX_TEXT_SIZE_SP, textSizeSp));
        super.setTextSize(TypedValue.COMPLEX_UNIT_SP, safeSize);
        lineNumberPaint.setTextSize(getTextSize() * 0.9f);
        requestLayout();
        invalidate();
    }

    private float getCurrentTextSizeSp() {
        return getTextSize() / getResources().getDisplayMetrics().scaledDensity;
    }

    private void resetTextGestureIfFinished(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            resetTextGestureState();
        }
    }

    private void resetTextGestureState() {
        textGestureActive = true;
        allowLongPressSelection = true;
        setLongClickable(true);
    }

    private boolean isTouchOnText(MotionEvent event) {
        Layout layout = getLayout();
        CharSequence text = getText();
        if (layout == null || TextUtils.isEmpty(text)) {
            return false;
        }
        int lineCount = layout.getLineCount();
        if (lineCount <= 0) {
            return false;
        }

        float y = event.getY() + getScrollY() - getTotalPaddingTop();
        if (y < 0 || y > layout.getLineBottom(lineCount - 1)) {
            return false;
        }

        int line = layout.getLineForVertical((int) y);
        if (line < 0 || line >= lineCount) {
            return false;
        }

        int start = layout.getLineStart(line);
        int end = layout.getLineEnd(line);
        if (isBlankLine(text, start, end)) {
            return false;
        }

        float x = event.getX() + getScrollX() - getTotalPaddingLeft();
        int slop = dp(8);
        return x >= layout.getLineLeft(line) - slop
                && x <= layout.getLineRight(line) + slop;
    }

    private boolean isBlankLine(CharSequence text, int start, int end) {
        int safeEnd = end;
        while (safeEnd > start) {
            char ch = text.charAt(safeEnd - 1);
            if (ch == '\n' || ch == '\r') {
                safeEnd--;
            } else {
                break;
            }
        }
        for (int index = start; index < safeEnd; index++) {
            if (!Character.isWhitespace(text.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
