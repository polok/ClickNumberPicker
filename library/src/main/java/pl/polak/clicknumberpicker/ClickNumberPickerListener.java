package pl.polak.clicknumberpicker;

/**
 * Click number picker listener
 */
public interface ClickNumberPickerListener {

    /**
     * Listen on picker value change
     * @param previousValue of the picker
     * @param currentValue of the picker
     * @param pickerClickType tells if value was increased on decreased
     */
    void onValueChange(float previousValue, float currentValue, PickerClickType pickerClickType);

}
