# Android Auto Resize Text View

### Overview

Android provide auto sizing functionality of TextView since from Android 8.0.
However, it required fixed height or fixed width implementation, in order to make it works.
In additional, this functionality is not fully supported in RecyclerView.

There are lots of custom implementation for TextView to achieve auto sizing, but most of them are difficult to use or customize.

### Idea

- Timing for calculation
  According to Android Developers website, size of the view will be calcalculated after `onMeasure(int, int)` is called.
  That means calculation of text size should be done after this.
  After several experiments, `onLayout(boolean, int, int, int, int)` is the best timing to calculate suitable text size.

- Calculation
  Most suggested solutions from different platforms, (such as Stackoverflow, Medium, etc.) are doing calculation by linear search, with following pseudo code
  ```
  textSize = max
  while (it is too big && textSize > min) {
      textSize *= 0.9
  }
  apply textSize
  ```

  Since this calculation is running on main thread, I would prefer binary search to make it faster.
  Calculation is pixel-based.

- Minimize customization part
  In order to make implementation easier to understand and customize, customization part should be as simple as possible.

- Invalidation of layout
  No matter what kind of implementation has been used, `requestLayout()` must be called, in order to refresh layout bounds of view.

### Explanation

- `1f` or `android:autoSizeMinTextSize` will be taken as min text size
- `android:autoSizeMaxTextSize` or `android:textSize` will be taken as max text size
- text will be shown as big as possible
- TextView's height will be updated based on text size change

### Integration Guideline

```
<!-- Example for match_parent -->
<com.codedeco.lib.ui.widget.textview.AutoSizeTextView
    android:layout_height="wrap_content"
    android:layout_width="match_parent"
    android:text="add entry"
    android:autoSizeMinTextSize="6sp"
    android:textSize="16sp"
    android:maxLine="1" />

<!-- Example of match_constraint -->
<com.codedeco.lib.ui.widget.textview.AutoSizeTextView
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintEnd_toEndOf="parent"
    android:layout_height="wrap_content"
    android:layout_width="0dp"
    android:text="add entry"
    android:autoSizeMinTextSize="6sp"
    android:textSize="16sp"
    android:maxLine="1" />

<!-- Example of fixed size -->
<com.codedeco.lib.ui.widget.textview.AutoSizeTextView
    android:layout_height="wrap_content"
    android:layout_width="100dp"
    android:text="add entry"
    android:autoSizeMinTextSize="6sp"
    android:textSize="16sp"
    android:maxLine="1" />
```

### Known issues

- IDE cannot shows exactly height of View in Design View.

### References

[Autosize TextViews](https://developer.android.com/develop/ui/views/text-and-emoji/autosizing-textview)

[View | Android Developers](https://developer.android.com/reference/android/view/View)

[Binary Search Algorithm – Iterative and Recursive Implementation | GeeksforGeeks](https://www.geeksforgeeks.org/binary-search/)

### Index

- [Overview](#overview)
