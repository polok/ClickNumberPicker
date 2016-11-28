package pl.polak.clicknumberpicker;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.support.percent.PercentRelativeLayout;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Locale;

/**
 * I am click number picker view
 */
public class ClickNumberPickerView extends PercentRelativeLayout {

    private static final float CLICK_NUMBER_PICKER_MIN_VALUE_DEFAULT = 0.0f;
    private static final float CLICK_NUMBER_PICKER_MAX_VALUE_DEFAULT = 100;
    private static final float CLICK_NUMBER_PICKER_VALUE_DEFAULT = 0;
    private static final float CLICK_NUMBER_PICKER_STEP_DEFAULT = 1;
    private static final int CLICK_NUMBER_PICKER_VALUE_TEXT_SIZE_DEFAULT = 15;
    private static final int CLICK_NUMBER_PICKER_VALUE_ANIMATION_MIN_TEXT_SIZE_DEFAULT = 10;
    private static final int CLICK_NUMBER_PICKER_VALUE_ANIMATION_MAX_TEXT_SIZE_DEFAULT = 22;
    private static final int CLICK_NUMBER_PICKER_VALUE_VIEW_OFFSET_DEFAULT = 20;
    private static final float CLICK_NUMBER_PICKER_CORNER_RADIUS_DEFAULT = 10;
    private static final int CLICK_NUMBER_PICKER_DECIMAL_NUMBER_DEFAULT = 2;
    private static final int CLICK_NUMBER_PICKER_UP_DOWN_DURATION_DEFAULT = 200;
    private static final int CLICK_NUMBER_PICKER_OFFSET_ANIMATION_DURATION_DEFAULT = 150;

    private FrameLayout flLeftPicker;
    private FrameLayout flRightPicker;
    private TextView tvValue;
    private PercentRelativeLayout rlRootView;
    private RelativeLayout rlCenter;

    private boolean swipeEnabled;
    private float value;
    private float minValue;
    private float maxValue;
    private float step;
    private boolean integerPriority;
    private int valueBackgroundColor;
    private int pickersBackgroundColor;
    private int animationUpDuration;
    private int animationDownDuration;
    private boolean animationUpEnabled;
    private boolean animationDownEnabled;
    private boolean animationSwipeEnabled;
    private int valueColor;
    private int valueTextSize;
    private int valueMinTextSize;
    private int valueMaxTextSize;
    private float valueViewOffset;
    private float pickerCornerRadius;
    private int pickerBorderStrokeWidth;
    private int pickerBorderStrokeColor;
    private int decimalNumbers;
    private int animationOffsetLeftDuration;
    private int animationOffsetRightDuration;
    private int leftPickerLayout;
    private int rightPickerLayout;

    private ClickNumberPickerListener clickNumberPickerListener = new ClickNumberPickerListener() {
        @Override
        public void onValueChange(float previousValue, float currentValue, PickerClickType pickerClickType) {}
    };

    private ObjectAnimator leftPickerTranslationXAnimator;
    private ObjectAnimator rightPickerTranslationXAnimator;
    private ValueAnimator valueUpChangeAnimator;
    private ValueAnimator valueDownChangeAnimator;

    private String valueFormatter;
    private Handler swipeValueChangeHandler = new Handler();
    private PickerClickType swipeDirection = PickerClickType.NONE;
    private float swipeStep = 1;

    private Runnable valueChangeRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                switch (swipeDirection) {
                    case LEFT:
                        updatePickerValueByStep(-(swipeStep * swipeStep));
                        break;
                    case RIGHT:
                        updatePickerValueByStep(swipeStep * swipeStep);
                        break;
                }
            } finally {
                ++swipeStep;
                swipeValueChangeHandler.postDelayed(valueChangeRunnable, 200);
            }
        }
    };

    private OnTouchListener touchListener = new OnTouchListener() {
        private float dX = 0.0f;
        private float initTouchX = 0.0f;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    swipeStep = 1;
                    dX = rlCenter.getX() - event.getRawX();
                    initTouchX = rlCenter.getX();
                    break;

                case MotionEvent.ACTION_MOVE:
                    if ((initTouchX - valueViewOffset * 2) > event.getRawX() + dX){
                        swipeDirection = PickerClickType.LEFT;
                        valueChangeRunnable.run();
                        break;
                    } else if ((initTouchX + valueViewOffset * 2) < event.getRawX() + dX) {
                        swipeDirection = PickerClickType.RIGHT;
                        valueChangeRunnable.run();
                        break;
                    }

                    rlCenter.animate()
                            .x(event.getRawX() + dX)
                            .setDuration(0)
                            .start();
                    break;
                case MotionEvent.ACTION_UP:
                    swipeValueChangeHandler.removeCallbacks(valueChangeRunnable);

                    rlCenter.animate()
                            .x(initTouchX)
                            .setDuration(250)
                            .start();
                    break;
                default:
                    return false;
            }

            return true;
        }
    };

    private OnClickListener leftPickerListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if (animationSwipeEnabled) {
                if (leftPickerTranslationXAnimator.isRunning()) {
                    leftPickerTranslationXAnimator.end();
                }

                leftPickerTranslationXAnimator.start();
            }

            if (animationDownEnabled) {
                if (valueDownChangeAnimator.isRunning()) {
                    valueDownChangeAnimator.end();
                }
                valueDownChangeAnimator.start();
            }


            updatePickerValueByStep(-step);
        }
    };

    private OnClickListener rightPickerListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if (animationSwipeEnabled) {
                if (rightPickerTranslationXAnimator.isRunning()) {
                    rightPickerTranslationXAnimator.end();
                }
                rightPickerTranslationXAnimator.start();
            }

            if (animationUpEnabled) {
                if (valueUpChangeAnimator.isRunning()) {
                    valueUpChangeAnimator.end();
                }
                valueUpChangeAnimator.start();
            }

            updatePickerValueByStep(step);
        }
    };

    public ClickNumberPickerView(Context context) {
        this(context, null);
    }

    public ClickNumberPickerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClickNumberPickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        readAttributes(context, attrs);

        init();
    }

    private void readAttributes(Context context, AttributeSet attrs) {
        if (attrs == null) {
            return;
        }

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ClickNumberPicker);

        swipeEnabled = typedArray.getBoolean(R.styleable.ClickNumberPicker_swipe_enabled, true);
        value = typedArray.getFloat(R.styleable.ClickNumberPicker_value, CLICK_NUMBER_PICKER_VALUE_DEFAULT);
        minValue = typedArray.getFloat(R.styleable.ClickNumberPicker_min_value, CLICK_NUMBER_PICKER_MIN_VALUE_DEFAULT);
        maxValue = typedArray.getFloat(R.styleable.ClickNumberPicker_max_value, CLICK_NUMBER_PICKER_MAX_VALUE_DEFAULT);
        step = typedArray.getFloat(R.styleable.ClickNumberPicker_step, CLICK_NUMBER_PICKER_STEP_DEFAULT);
        integerPriority = typedArray.getBoolean(R.styleable.ClickNumberPicker_integer_priority, false);
        valueBackgroundColor = typedArray.getColor(R.styleable.ClickNumberPicker_value_background_color, 0);
        pickersBackgroundColor = typedArray.getColor(R.styleable.ClickNumberPicker_pickers_background_color, 0);
        animationUpEnabled = typedArray.getBoolean(R.styleable.ClickNumberPicker_value_animation_up, false);
        animationDownEnabled = typedArray.getBoolean(R.styleable.ClickNumberPicker_value_animation_down, false);
        valueColor = typedArray.getColor(R.styleable.ClickNumberPicker_value_text_color, 0);
        valueTextSize = typedArray.getDimensionPixelSize(R.styleable.ClickNumberPicker_value_text_size, CLICK_NUMBER_PICKER_VALUE_TEXT_SIZE_DEFAULT);
        valueMinTextSize = typedArray.getDimensionPixelSize(R.styleable.ClickNumberPicker_value_min_text_size, CLICK_NUMBER_PICKER_VALUE_ANIMATION_MIN_TEXT_SIZE_DEFAULT);
        valueMaxTextSize = typedArray.getDimensionPixelSize(R.styleable.ClickNumberPicker_value_max_text_size, CLICK_NUMBER_PICKER_VALUE_ANIMATION_MAX_TEXT_SIZE_DEFAULT);
        valueViewOffset = typedArray.getFloat(R.styleable.ClickNumberPicker_value_view_offset, CLICK_NUMBER_PICKER_VALUE_VIEW_OFFSET_DEFAULT);
        animationSwipeEnabled = typedArray.getBoolean(R.styleable.ClickNumberPicker_swipe_animation, false);
        pickerCornerRadius = typedArray.getFloat(R.styleable.ClickNumberPicker_picker_corner_radius, CLICK_NUMBER_PICKER_CORNER_RADIUS_DEFAULT);
        pickerBorderStrokeWidth = typedArray.getInt(R.styleable.ClickNumberPicker_picker_border_stroke_width, 0);
        pickerBorderStrokeColor = typedArray.getColor(R.styleable.ClickNumberPicker_picker_border_stroke_color, 0);
        decimalNumbers = typedArray.getInt(R.styleable.ClickNumberPicker_decimal_number, CLICK_NUMBER_PICKER_DECIMAL_NUMBER_DEFAULT);
        animationUpDuration = typedArray.getInt(R.styleable.ClickNumberPicker_animation_value_up_duration, CLICK_NUMBER_PICKER_UP_DOWN_DURATION_DEFAULT);
        animationDownDuration = typedArray.getInt(R.styleable.ClickNumberPicker_animation_value_down_duration, CLICK_NUMBER_PICKER_UP_DOWN_DURATION_DEFAULT);
        animationOffsetRightDuration = typedArray.getInt(R.styleable.ClickNumberPicker_animation_offset_right_duration, CLICK_NUMBER_PICKER_OFFSET_ANIMATION_DURATION_DEFAULT);
        animationOffsetLeftDuration = typedArray.getInt(R.styleable.ClickNumberPicker_animation_offset_left_duration, CLICK_NUMBER_PICKER_OFFSET_ANIMATION_DURATION_DEFAULT);
        leftPickerLayout = typedArray.getResourceId(R.styleable.ClickNumberPicker_left_picker_layout, R.layout.left_picker_view_default);
        rightPickerLayout = typedArray.getResourceId(R.styleable.ClickNumberPicker_right_picker_layout, R.layout.right_picker_view_default);
        typedArray.recycle();
    }

    private void init() {
        initViews();
        initAnimators();
        initListeners();

        applyViewAttributes();
    }

    private void applyViewAttributes() {
        GradientDrawable gd = (GradientDrawable) rlRootView.getBackground().getCurrent();
        gd.setColor(pickersBackgroundColor);
        gd.setCornerRadius(pickerCornerRadius);
        gd.setStroke(pickerBorderStrokeWidth, pickerBorderStrokeColor);

        valueFormatter = NumberFormatUtils.provideFloatFormater(decimalNumbers);
        swipeStep = step;
        rlCenter.setBackgroundColor(valueBackgroundColor);
        tvValue.setTextColor(valueColor);
        setPickerValue(value);
    }

    private void initListeners() {
        if(swipeEnabled) {
            rlCenter.setOnTouchListener(touchListener);
        }

        flLeftPicker.setOnClickListener(leftPickerListener);
        flRightPicker.setOnClickListener(rightPickerListener);
    }

    private void initViews() {
        View view = inflate(getContext(), R.layout.view_click_numberpicker, this);

        flLeftPicker = (FrameLayout) view.findViewById(R.id.fl_click_numberpicker_left);
        rlRootView = (PercentRelativeLayout) view.findViewById(R.id.rl_pickers_root);
        flRightPicker = (FrameLayout) view.findViewById(R.id.fl_click_numberpicker_right);
        rlCenter = (RelativeLayout) view.findViewById(R.id.center_picker);
        tvValue = (TextView) view.findViewById(R.id.tv_value_numberpicker);

        View leftPickerView = inflate(getContext(), leftPickerLayout, null);
        flLeftPicker.addView(leftPickerView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        View rightPickerView = inflate(getContext(), rightPickerLayout, null);
        flRightPicker.addView(rightPickerView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private void initAnimators() {
        leftPickerTranslationXAnimator = ObjectAnimator.ofFloat(rlCenter, "translationX", -valueViewOffset);
        leftPickerTranslationXAnimator.setInterpolator(new FastOutLinearInInterpolator());
        leftPickerTranslationXAnimator.setRepeatMode(ValueAnimator.REVERSE);
        leftPickerTranslationXAnimator.setRepeatCount(1);
        leftPickerTranslationXAnimator.setDuration(animationOffsetLeftDuration);

        rightPickerTranslationXAnimator = ObjectAnimator.ofFloat(rlCenter, "translationX", valueViewOffset);
        rightPickerTranslationXAnimator.setInterpolator(new FastOutLinearInInterpolator());
        rightPickerTranslationXAnimator.setRepeatMode(ValueAnimator.REVERSE);
        rightPickerTranslationXAnimator.setRepeatCount(1);
        rightPickerTranslationXAnimator.setDuration(animationOffsetRightDuration);

        valueDownChangeAnimator = ObjectAnimator.ofFloat(tvValue, "textSize", valueTextSize, valueMinTextSize);
        valueDownChangeAnimator.setDuration(animationDownDuration);
        valueDownChangeAnimator.setRepeatCount(1);
        valueDownChangeAnimator.setRepeatMode(ValueAnimator.REVERSE);

        valueUpChangeAnimator = ObjectAnimator.ofFloat(tvValue, "textSize", valueTextSize, valueMaxTextSize);
        valueUpChangeAnimator.setDuration(animationUpDuration);
        valueUpChangeAnimator.setRepeatCount(1);
        valueUpChangeAnimator.setRepeatMode(ValueAnimator.REVERSE);
    }

    public void setPickerValue(float value) {
        if(value < minValue || value > maxValue) {
            return;
        }

        clickNumberPickerListener.onValueChange(this.value, value, this.value > value ? PickerClickType.LEFT : PickerClickType.RIGHT);

        this.value = value;
        tvValue.setText(formatValue(this.value));
    }

    private String formatValue(float value) {
        if(integerPriority && Math.round(value) == value) {
            return String.format(Locale.US, "%.0f", value);
        }

        return String.format(Locale.US, valueFormatter, value);
    }

    public void updatePickerValueByStep(float step) {
        if(value + step < minValue) {
            setPickerValue(minValue);
        } else if (value + step > maxValue) {
            setPickerValue(maxValue);
        }

        setPickerValue(value + step);
    }

    /**
     * Set picker number value change listener
     * @param clickNumberPickerListener
     */
    public void setClickNumberPickerListener(ClickNumberPickerListener clickNumberPickerListener) {
        this.clickNumberPickerListener = clickNumberPickerListener;
    }
}
