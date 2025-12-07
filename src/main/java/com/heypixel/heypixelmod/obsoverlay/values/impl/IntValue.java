package com.heypixel.heypixelmod.obsoverlay.values.impl;

import com.heypixel.heypixelmod.obsoverlay.utils.MathUtils;
import com.heypixel.heypixelmod.obsoverlay.values.HasValue;
import com.heypixel.heypixelmod.obsoverlay.values.Value;
import com.heypixel.heypixelmod.obsoverlay.values.ValueType;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class IntValue extends Value {
   private final int defaultValue;
   private final int minValue;
   private final int maxValue;
   private final int step;
   private final Consumer<Value> update;
   private int currentValue;

   public IntValue(
      HasValue key, String name, int defaultValue, int minValue, int maxValue, int step, Consumer<Value> update, Supplier<Boolean> visibility
   ) {
      super(key, name, visibility);
      this.update = update;
      this.currentValue = this.defaultValue = defaultValue;
      this.minValue = minValue;
      this.maxValue = maxValue;
      this.step = step;
   }

   @Override
   public ValueType getValueType() {
      return ValueType.INTEGER;
   }

   public IntValue getIntValue() {
      return this;
   }

   public void setCurrentValue(int currentValue) {
      this.currentValue = MathUtils.clampValue(currentValue, this.minValue, this.maxValue);
      if (this.update != null) {
         this.update.accept(this);
      }
   }

   public int getDefaultValue() {
      return this.defaultValue;
   }

   public int getMinValue() {
      return this.minValue;
   }

   public int getMaxValue() {
      return this.maxValue;
   }

   public int getStep() {
      return this.step;
   }

   public Consumer<Value> getUpdate() {
      return this.update;
   }

   public int getCurrentValue() {
      return this.currentValue;
   }
}