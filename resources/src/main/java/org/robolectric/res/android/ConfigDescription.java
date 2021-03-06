package org.robolectric.res.android;

import static org.robolectric.res.android.Util.isTruthy;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * From android/frameworks/base/tools/aapt2/ConfigDescription.cpp
 */
public class ConfigDescription {
  public static int SDK_CUPCAKE = 3;
  public static int SDK_DONUT = 4;
  public static int SDK_ECLAIR = 5;
  public static int SDK_ECLAIR_0_1 = 6;
  public static int SDK_ECLAIR_MR1 = 7;
  public static int SDK_FROYO = 8;
  public static int SDK_GINGERBREAD = 9;
  public static int SDK_GINGERBREAD_MR1 = 10;
  public static int SDK_HONEYCOMB = 11;
  public static int SDK_HONEYCOMB_MR1 = 12;
  public static int SDK_HONEYCOMB_MR2 = 13;
  public static int SDK_ICE_CREAM_SANDWICH = 14;
  public static int SDK_ICE_CREAM_SANDWICH_MR1 = 15;
  public static int SDK_JELLY_BEAN = 16;
  public static int SDK_JELLY_BEAN_MR1 = 17;
  public static int SDK_JELLY_BEAN_MR2 = 18;
  public static int SDK_KITKAT = 19;
  public static int SDK_KITKAT_WATCH = 20;
  public static int SDK_LOLLIPOP = 21;
  public static int SDK_LOLLIPOP_MR1 = 22;
  public static int SDK_MNC = 23;
  public static int SDK_NOUGAT = 24;
  public static int SDK_NOUGAT_MR1 = 25;
  public static int SDK_O = 26;

  /**
   * finalant used to to represent MNC (Mobile Network Code) zero.
   * 0 cannot be used, since it is used to represent an undefined MNC.
   */
  private static final int ACONFIGURATION_MNC_ZERO = 0xffff;

  private static final String kWildcardName = "any";

  private static final Pattern MCC_PATTERN = Pattern.compile("mcc([\\d]+)");
  private static final Pattern MNC_PATTERN = Pattern.compile("mnc([\\d]+)");
  private static final Pattern SMALLEST_SCREEN_WIDTH_PATTERN = Pattern.compile("^sw([0-9]+)dp");
  private static final Pattern SCREEN_WIDTH_PATTERN = Pattern.compile("^w([0-9]+)dp");
  private static final Pattern SCREEN_HEIGHT_PATTERN = Pattern.compile("^h([0-9]+)dp");
  private static final Pattern DENSITY_PATTERN = Pattern.compile("^([0-9]+)dpi");
  private static final Pattern HEIGHT_WIDTH_PATTERN = Pattern.compile("^([0-9]+)x([0-9]+)");
  private static final Pattern VERSION_QUALIFIER_PATTERN = Pattern.compile("v([0-9]+)$");

  public static class LocaleValue {

    String language;
    String region;
    String script;
    String variant;

    void set_language(String language_chars) {
      language = language_chars.trim().toLowerCase();
    }

    void set_region(String region_chars) {
      region = region_chars.trim().toUpperCase();
    }

    void set_script(String script_chars) {
      script = String.valueOf(Character.toUpperCase(script_chars.charAt(0))) +
          script_chars.substring(1).toLowerCase();
    }

    void set_variant(String variant_chars) {
      variant = variant_chars.trim();
    }


    static boolean is_alpha(final String str) {
      for (int i = 0; i < str.length(); i++) {
        if (!Character.isAlphabetic(str.charAt(i))) {
          return false;
        }
      }

      return true;
    }

    int initFromParts(PeekingIterator<String> iter) {

      String part = iter.peek();
      if (part.startsWith("b+")) {
        // This is a "modified" BCP 47 language tag. Same semantics as BCP 47 tags,
        // except that the separator is "+" and not "-".
        String[] subtags = part.substring(2).toLowerCase().split("\\+");
        if (subtags.length == 1) {
          set_language(subtags[0]);
        } else if (subtags.length == 2) {
          set_language(subtags[0]);

          // The second tag can either be a region, a variant or a script.
          switch (subtags[1].length()) {
            case 2:
            case 3:
              set_region(subtags[1]);
              break;
            case 4:
              if ('0' <= subtags[1].charAt(0) && subtags[1].charAt(0) <= '9') {
                // This is a variant: fall through
              } else {
                set_script(subtags[1]);
                break;
              }
              // fall through
            case 5:
            case 6:
            case 7:
            case 8:
              set_variant(subtags[1]);
              break;
            default:
              return -1;
          }
        } else if (subtags.length == 3) {
          // The language is always the first subtag.
          set_language(subtags[0]);

          // The second subtag can either be a script or a region code.
          // If its size is 4, it's a script code, else it's a region code.
          if (subtags[1].length() == 4) {
            set_script(subtags[1]);
          } else if (subtags[1].length() == 2 || subtags[1].length() == 3) {
            set_region(subtags[1]);
          } else {
            return -1;
          }

          // The third tag can either be a region code (if the second tag was
          // a script), else a variant code.
          if (subtags[2].length() >= 4) {
            set_variant(subtags[2]);
          } else {
            set_region(subtags[2]);
          }
        } else if (subtags.length == 4) {
          set_language(subtags[0]);
          set_script(subtags[1]);
          set_region(subtags[2]);
          set_variant(subtags[3]);
        } else {
          return -1;
        }

        iter.next();

      } else {
        if ((part.length() == 2 || part.length() == 3) && is_alpha(part) &&
            !Objects.equals(part, "car")) {
          set_language(part);
          iter.next();

          if (iter.hasNext()) {
            final String region_part = iter.peek();
            if (region_part.charAt(0) == 'r' && region_part.length() == 3) {
              set_region(region_part.substring(1));
              iter.next();
            }
          }
        }
      }

      return 0;
    }

    public void writeTo(ResTable_config out) {
      out.packLanguage(language);
      out.packRegion(region);

      Arrays.fill(out.localeScript, (byte) 0);
      byte[] scriptBytes = script == null ? new byte[4] : script.getBytes();
      System.arraycopy(scriptBytes, 0, out.localeScript, 0, scriptBytes.length);

      Arrays.fill(out.localeVariant, (byte) 0);
      byte[] variantBytes = variant == null ? new byte[8] : variant.getBytes();
      System.arraycopy(variantBytes, 0, out.localeVariant, 0, variantBytes.length);
    }
  }

  public boolean parse(final String str, ResTable_config out) {
    return parse(str, out, true);
  }

  public boolean parse(final String str, ResTable_config out, boolean applyVersionForCompat) {
    PeekingIterator<String> part_iter = Iterators
        .peekingIterator(Arrays.asList(str.toLowerCase().split("-")).iterator());

    LocaleValue locale = new LocaleValue();

    boolean success = !part_iter.hasNext();
    if (part_iter.hasNext() && parseMcc(part_iter.peek(), out)) {
      part_iter.next();
      if (!part_iter.hasNext()) {
        success = !part_iter.hasNext();
      }
    }

    if (part_iter.hasNext() && parseMnc(part_iter.peek(), out)) {
      part_iter.next();
      if (!part_iter.hasNext()) {
        success = !part_iter.hasNext();
      }
    }

    if (part_iter.hasNext()) {
      // Locale spans a few '-' separators, so we let it
      // control the index.
      int parts_consumed = locale.initFromParts(part_iter);
      if (parts_consumed < 0) {
        return false;
      } else {
        locale.writeTo(out);
        if (!part_iter.hasNext()) {
          success = !part_iter.hasNext();
        }
      }
    }

    if (part_iter.hasNext() && parseLayoutDirection(part_iter.peek(), out)) {
      part_iter.next();
      if (!part_iter.hasNext()) {
        success = !part_iter.hasNext();
      }
    }

    if (part_iter.hasNext() && parseSmallestScreenWidthDp(part_iter.peek(), out)) {
      part_iter.next();
      if (!part_iter.hasNext()) {
        success = !part_iter.hasNext();
      }
    }

    if (part_iter.hasNext() && parseScreenWidthDp(part_iter.peek(), out)) {
      part_iter.next();
      if (!part_iter.hasNext()) {
        success = !part_iter.hasNext();
      }
    }

    if (part_iter.hasNext() && parseScreenHeightDp(part_iter.peek(), out)) {
      part_iter.next();
      if (!part_iter.hasNext()) {
        success = !part_iter.hasNext();
      }
    }

    if (part_iter.hasNext() && parseScreenLayoutSize(part_iter.peek(), out)) {
      part_iter.next();
      if (!part_iter.hasNext()) {
        success = !part_iter.hasNext();
      }
    }

    if (part_iter.hasNext() && parseScreenLayoutLong(part_iter.peek(), out)) {
      part_iter.next();
      if (!part_iter.hasNext()) {
        success = !part_iter.hasNext();
      }
    }

    if (part_iter.hasNext() && parseScreenRound(part_iter.peek(), out)) {
      part_iter.next();
      if (!part_iter.hasNext()) {
        success = !part_iter.hasNext();
      }
    }

    if (part_iter.hasNext() && parseOrientation(part_iter.peek(), out)) {
      part_iter.next();
      if (!part_iter.hasNext()) {
        success = !part_iter.hasNext();
      }
    }

    if (part_iter.hasNext() && parseUiModeType(part_iter.peek(), out)) {
      part_iter.next();
      if (!part_iter.hasNext()) {
        success = !part_iter.hasNext();
      }
    }

    if (part_iter.hasNext() && parseUiModeNight(part_iter.peek(), out)) {
      part_iter.next();
      if (!part_iter.hasNext()) {
        success = !part_iter.hasNext();
      }
    }

    if (part_iter.hasNext() && parseDensity(part_iter.peek(), out)) {
      part_iter.next();
      if (!part_iter.hasNext()) {
        success = !part_iter.hasNext();
      }
    }

    if (part_iter.hasNext() && parseTouchscreen(part_iter.peek(), out)) {
      part_iter.next();
      if (!part_iter.hasNext()) {
        success = !part_iter.hasNext();
      }
    }

    if (part_iter.hasNext() && parseKeysHidden(part_iter.peek(), out)) {
      part_iter.next();
      if (!part_iter.hasNext()) {
        success = !part_iter.hasNext();
      }
    }

    if (part_iter.hasNext() && parseKeyboard(part_iter.peek(), out)) {
      part_iter.next();
      if (!part_iter.hasNext()) {
        success = !part_iter.hasNext();
      }
    }

    if (part_iter.hasNext() && parseNavHidden(part_iter.peek(), out)) {
      part_iter.next();
      if (!part_iter.hasNext()) {
        success = !part_iter.hasNext();
      }
    }

    if (part_iter.hasNext() && parseNavigation(part_iter.peek(), out)) {
      part_iter.next();
      if (!part_iter.hasNext()) {
        success = !part_iter.hasNext();
      }
    }

    if (part_iter.hasNext() && parseScreenSize(part_iter.peek(), out)) {
      part_iter.next();
      if (!part_iter.hasNext()) {
        success = !part_iter.hasNext();
      }
    }

    if (part_iter.hasNext() && parseVersion(part_iter.peek(), out)) {
      part_iter.next();
      if (!part_iter.hasNext()) {
        success = !part_iter.hasNext();
      }
    }

    if (!success) {
      // Unrecognized.
      return false;
    }

    if (out != null && applyVersionForCompat) {
      applyVersionForCompatibility(out);
    }
    return true;
  }

  private boolean parseLayoutDirection(String name, ResTable_config out) {
    if (Objects.equals(name, kWildcardName)) {
      if (out != null) {
        out.screenLayout =
            (out.screenLayout & ~ResTable_config.MASK_LAYOUTDIR) |
                ResTable_config.LAYOUTDIR_ANY;
      }
      return true;
    } else if (Objects.equals(name, "ldltr")) {
      if (out != null) {
        out.screenLayout =
            (out.screenLayout & ~ResTable_config.MASK_LAYOUTDIR) |
                ResTable_config.LAYOUTDIR_LTR;
      }
      return true;
    } else if (Objects.equals(name, "ldrtl")) {
      if (out != null) {
        out.screenLayout =
            (out.screenLayout & ~ResTable_config.MASK_LAYOUTDIR) |
                ResTable_config.LAYOUTDIR_RTL;
      }
      return true;
    }

    return false;
  }

  private boolean parseSmallestScreenWidthDp(String name, ResTable_config out) {
    if (Objects.equals(name, kWildcardName)) {
      if (out != null) {
        out.smallestScreenWidthDp = ResTable_config.SCREENWIDTH_ANY;
      }
      return true;
    }

    Matcher matcher = SMALLEST_SCREEN_WIDTH_PATTERN.matcher(name);
    if (matcher.matches()) {
      out.smallestScreenWidthDp = Integer.parseInt(matcher.group(1));
      return true;
    }
    return false;
  }

  private boolean parseScreenWidthDp(String name, ResTable_config out) {
    if (Objects.equals(name, kWildcardName)) {
      if (out != null) {
        out.screenWidthDp = ResTable_config.SCREENWIDTH_ANY;
      }
      return true;
    }

    Matcher matcher = SCREEN_WIDTH_PATTERN.matcher(name);
    if (matcher.matches()) {
      out.screenWidthDp = Integer.parseInt(matcher.group(1));
      return true;
    }
    return false;
  }

  private boolean parseScreenHeightDp(String name, ResTable_config out) {
    if (Objects.equals(name, kWildcardName)) {
      if (out != null) {
        out.screenHeightDp = ResTable_config.SCREENWIDTH_ANY;
      }
      return true;
    }

    Matcher matcher = SCREEN_HEIGHT_PATTERN.matcher(name);
    if (matcher.matches()) {
      out.screenHeightDp = Integer.parseInt(matcher.group(1));
      return true;
    }
    return false;
  }

  private boolean parseScreenLayoutSize(String name, ResTable_config out) {
    if (Objects.equals(name, kWildcardName)) {
      if (out != null) {
        out.screenLayout =
            (out.screenLayout & ~ResTable_config.MASK_SCREENSIZE) |
                ResTable_config.SCREENSIZE_ANY;
      }
      return true;
    } else if (Objects.equals(name, "small")) {
      if (out != null) {
        out.screenLayout =
            (out.screenLayout & ~ResTable_config.MASK_SCREENSIZE) |
                ResTable_config.SCREENSIZE_SMALL;
      }
      return true;
    } else if (Objects.equals(name, "normal")) {
      if (out != null) {
        out.screenLayout =
            (out.screenLayout & ~ResTable_config.MASK_SCREENSIZE) |
                ResTable_config.SCREENSIZE_NORMAL;
      }
      return true;
    } else if (Objects.equals(name, "large")) {
      if (out != null) {
        out.screenLayout =
            (out.screenLayout & ~ResTable_config.MASK_SCREENSIZE) |
                ResTable_config.SCREENSIZE_LARGE;
      }
      return true;
    } else if (Objects.equals(name, "xlarge")) {
      if (out != null) {
        out.screenLayout =
            (out.screenLayout & ~ResTable_config.MASK_SCREENSIZE) |
                ResTable_config.SCREENSIZE_XLARGE;
      }
      return true;
    }

    return false;
  }

  static boolean parseScreenLayoutLong(final String name, ResTable_config out) {
    if (Objects.equals(name, kWildcardName)) {
      if (out != null) {
        out.screenLayout =
            (out.screenLayout&~ResTable_config.MASK_SCREENLONG)
                | ResTable_config.SCREENLONG_ANY;
      }
      return true;
    } else if (Objects.equals(name, "long")) {
      if (out != null) out.screenLayout =
          (out.screenLayout&~ResTable_config.MASK_SCREENLONG)
              | ResTable_config.SCREENLONG_YES;
      return true;
    } else if (Objects.equals(name, "notlong")) {
      if (out != null) out.screenLayout =
          (out.screenLayout&~ResTable_config.MASK_SCREENLONG)
              | ResTable_config.SCREENLONG_NO;
      return true;
    }
    return false;
  }

  private boolean parseScreenRound(String name, ResTable_config out) {
    if (Objects.equals(name, kWildcardName)) {
      if (out != null) {
        out.screenLayout2 =
            (byte) ((out.screenLayout2 & ~ResTable_config.MASK_SCREENROUND) |
                            ResTable_config.SCREENROUND_ANY);
      }
      return true;
    } else if (Objects.equals(name, "round")) {
      if (out != null) {
        out.screenLayout2 =
            (byte) ((out.screenLayout2 & ~ResTable_config.MASK_SCREENROUND) |
                            ResTable_config.SCREENROUND_YES);
      }
      return true;
    } else if (Objects.equals(name, "notround")) {
      if (out != null) {
        out.screenLayout2 =
            (byte) ((out.screenLayout2 & ~ResTable_config.MASK_SCREENROUND) |
                ResTable_config.SCREENROUND_NO);
      }
      return true;
    }
    return false;
  }

  private boolean parseOrientation(String name, ResTable_config out) {
    if (Objects.equals(name, kWildcardName)) {
      if (out != null) {
        out.orientation = out.ORIENTATION_ANY;
      }
      return true;
    } else if (Objects.equals(name, "port")) {
      if (out != null) {
        out.orientation = out.ORIENTATION_PORT;
      }
      return true;
    } else if (Objects.equals(name, "land")) {
      if (out != null) {
        out.orientation = out.ORIENTATION_LAND;
      }
      return true;
    } else if (Objects.equals(name, "square")) {
      if (out != null) {
        out.orientation = out.ORIENTATION_SQUARE;
      }
      return true;
    }

    return false;
  }

  private boolean parseUiModeType(String name, ResTable_config out) {
    if (Objects.equals(name, kWildcardName)) {
      if (out != null) {
        out.uiMode = (out.uiMode & ~ResTable_config.MASK_UI_MODE_TYPE) |
            ResTable_config.UI_MODE_TYPE_ANY;
      }
      return true;
    } else if (Objects.equals(name, "desk")) {
      if (out != null) {
        out.uiMode = (out.uiMode & ~ResTable_config.MASK_UI_MODE_TYPE) |
            ResTable_config.UI_MODE_TYPE_DESK;
      }
      return true;
    } else if (Objects.equals(name, "car")) {
      if (out != null) {
        out.uiMode = (out.uiMode & ~ResTable_config.MASK_UI_MODE_TYPE) |
            ResTable_config.UI_MODE_TYPE_CAR;
      }
      return true;
    } else if (Objects.equals(name, "television")) {
      if (out != null) {
        out.uiMode = (out.uiMode & ~ResTable_config.MASK_UI_MODE_TYPE) |
            ResTable_config.UI_MODE_TYPE_TELEVISION;
      }
      return true;
    } else if (Objects.equals(name, "appliance")) {
      if (out != null) {
        out.uiMode = (out.uiMode & ~ResTable_config.MASK_UI_MODE_TYPE) |
            ResTable_config.UI_MODE_TYPE_APPLIANCE;
      }
      return true;
    } else if (Objects.equals(name, "watch")) {
      if (out != null) {
        out.uiMode = (out.uiMode & ~ResTable_config.MASK_UI_MODE_TYPE) |
            ResTable_config.UI_MODE_TYPE_WATCH;
      }
      return true;
    }

    return false;
  }

  private boolean parseUiModeNight(String name, ResTable_config out) {
    if (Objects.equals(name, kWildcardName)) {
      if (out != null) {
        out.uiMode = (out.uiMode & ~ResTable_config.MASK_UI_MODE_NIGHT) |
            ResTable_config.UI_MODE_NIGHT_ANY;
      }
      return true;
    } else if (Objects.equals(name, "night")) {
      if (out != null) {
        out.uiMode = (out.uiMode & ~ResTable_config.MASK_UI_MODE_NIGHT) |
            ResTable_config.UI_MODE_NIGHT_YES;
      }
      return true;
    } else if (Objects.equals(name, "notnight")) {
      if (out != null) {
        out.uiMode = (out.uiMode & ~ResTable_config.MASK_UI_MODE_NIGHT) |
            ResTable_config.UI_MODE_NIGHT_NO;
      }
      return true;
    }

    return false;
  }

  private boolean parseDensity(String name, ResTable_config out) {
    if (Objects.equals(name, kWildcardName)) {
      if (out != null) {
        out.density = ResTable_config.DENSITY_DEFAULT;
      }
      return true;
    }

    if (Objects.equals(name, "anydpi")) {
      if (out != null) {
        out.density = ResTable_config.DENSITY_ANY;
      }
      return true;
    }

    if (Objects.equals(name, "nodpi")) {
      if (out != null) {
        out.density = ResTable_config.DENSITY_NONE;
      }
      return true;
    }

    if (Objects.equals(name, "ldpi")) {
      if (out != null) {
        out.density = ResTable_config.DENSITY_LOW;
      }
      return true;
    }

    if (Objects.equals(name, "mdpi")) {
      if (out != null) {
        out.density = ResTable_config.DENSITY_MEDIUM;
      }
      return true;
    }

    if (Objects.equals(name, "tvdpi")) {
      if (out != null) {
        out.density = ResTable_config.DENSITY_TV;
      }
      return true;
    }

    if (Objects.equals(name, "hdpi")) {
      if (out != null) {
        out.density = ResTable_config.DENSITY_HIGH;
      }
      return true;
    }

    if (Objects.equals(name, "xhdpi")) {
      if (out != null) {
        out.density = ResTable_config.DENSITY_XHIGH;
      }
      return true;
    }

    if (Objects.equals(name, "xxhdpi")) {
      if (out != null) {
        out.density = ResTable_config.DENSITY_XXHIGH;
      }
      return true;
    }

    if (Objects.equals(name, "xxxhdpi")) {
      if (out != null) {
        out.density = ResTable_config.DENSITY_XXXHIGH;
      }
      return true;
    }

    // check that we have 'dpi' after the last digit.
    Matcher matcher = DENSITY_PATTERN.matcher(name);
    if (matcher.matches()) {
      out.density = Integer.parseInt(matcher.group(1));
      return true;
    }
    return false;
  }

  private boolean parseTouchscreen(String name, ResTable_config out) {
    if (Objects.equals(name, kWildcardName)) {
      if (out != null) {
        out.touchscreen = out.TOUCHSCREEN_ANY;
      }
      return true;
    } else if (Objects.equals(name, "notouch")) {
      if (out != null) {
        out.touchscreen = out.TOUCHSCREEN_NOTOUCH;
      }
      return true;
    } else if (Objects.equals(name, "stylus")) {
      if (out != null) {
        out.touchscreen = out.TOUCHSCREEN_STYLUS;
      }
      return true;
    } else if (Objects.equals(name, "finger")) {
      if (out != null) {
        out.touchscreen = out.TOUCHSCREEN_FINGER;
      }
      return true;
    }

    return false;
  }

  private boolean parseKeysHidden(String name, ResTable_config out) {
    byte mask = 0;
    byte value = 0;
    if (Objects.equals(name, kWildcardName)) {
      mask = ResTable_config.MASK_KEYSHIDDEN;
      value = ResTable_config.KEYSHIDDEN_ANY;
    } else if (Objects.equals(name, "keysexposed")) {
      mask = ResTable_config.MASK_KEYSHIDDEN;
      value = ResTable_config.KEYSHIDDEN_NO;
    } else if (Objects.equals(name, "keyshidden")) {
      mask = ResTable_config.MASK_KEYSHIDDEN;
      value = ResTable_config.KEYSHIDDEN_YES;
    } else if (Objects.equals(name, "keyssoft")) {
      mask = ResTable_config.MASK_KEYSHIDDEN;
      value = ResTable_config.KEYSHIDDEN_SOFT;
    }

    if (mask != 0) {
      if (out != null) {
        out.inputFlags = (out.inputFlags & ~mask) | value;
      }
      return true;
    }

    return false;
  }

  private boolean parseKeyboard(String name, ResTable_config out) {
    if (Objects.equals(name, kWildcardName)) {
      if (out != null) {
        out.keyboard = out.KEYBOARD_ANY;
      }
      return true;
    } else if (Objects.equals(name, "nokeys")) {
      if (out != null) {
        out.keyboard = out.KEYBOARD_NOKEYS;
      }
      return true;
    } else if (Objects.equals(name, "qwerty")) {
      if (out != null) {
        out.keyboard = out.KEYBOARD_QWERTY;
      }
      return true;
    } else if (Objects.equals(name, "12key")) {
      if (out != null) {
        out.keyboard = out.KEYBOARD_12KEY;
      }
      return true;
    }

    return false;
  }

  private boolean parseNavHidden(String name, ResTable_config out) {
    byte mask = 0;
    byte value = 0;
    if (Objects.equals(name, kWildcardName)) {
      mask = ResTable_config.MASK_NAVHIDDEN;
      value = ResTable_config.NAVHIDDEN_ANY;
    } else if (Objects.equals(name, "navexposed")) {
      mask = ResTable_config.MASK_NAVHIDDEN;
      value = ResTable_config.NAVHIDDEN_NO;
    } else if (Objects.equals(name, "navhidden")) {
      mask = ResTable_config.MASK_NAVHIDDEN;
      value = ResTable_config.NAVHIDDEN_YES;
    }

    if (mask != 0) {
      if (out != null) {
        out.inputFlags = (out.inputFlags & ~mask) | value;
      }
      return true;
    }

    return false;
  }

  private boolean parseNavigation(String name, ResTable_config out) {
    if (Objects.equals(name, kWildcardName)) {
      if (out != null) {
        out.navigation = out.NAVIGATION_ANY;
      }
      return true;
    } else if (Objects.equals(name, "nonav")) {
      if (out != null) {
        out.navigation = out.NAVIGATION_NONAV;
      }
      return true;
    } else if (Objects.equals(name, "dpad")) {
      if (out != null) {
        out.navigation = out.NAVIGATION_DPAD;
      }
      return true;
    } else if (Objects.equals(name, "trackball")) {
      if (out != null) {
        out.navigation = out.NAVIGATION_TRACKBALL;
      }
      return true;
    } else if (Objects.equals(name, "wheel")) {
      if (out != null) {
        out.navigation = out.NAVIGATION_WHEEL;
      }
      return true;
    }

    return false;
  }

  private boolean parseScreenSize(String name, ResTable_config out) {
    if (Objects.equals(name, kWildcardName)) {
      if (out != null) {
        out.screenWidth = out.SCREENWIDTH_ANY;
        out.screenHeight = out.SCREENHEIGHT_ANY;
      }
      return true;
    }

    Matcher matcher = HEIGHT_WIDTH_PATTERN.matcher(name);
    if (matcher.matches()) {
      int w = Integer.parseInt(matcher.group(1));
      int h = Integer.parseInt(matcher.group(2));
      if (w < h) {
        return false;
      }
      out.screenWidth = w;
      out.screenHeight = h;
      return true;
    }
    return false;
  }

  private boolean parseVersion(String name, ResTable_config out) {
    if (Objects.equals(name, kWildcardName)) {
      if (out != null) {
        out.sdkVersion = out.SDKVERSION_ANY;
        out.minorVersion = out.MINORVERSION_ANY;
      }
      return true;
    }

    Matcher matcher = VERSION_QUALIFIER_PATTERN.matcher(name);
    if (matcher.matches()) {
      out.sdkVersion = Integer.parseInt(matcher.group(1));
      out.minorVersion = 0;
      return true;
    }
    return false;
  }

  private boolean parseMnc(String name, ResTable_config out) {
    if (Objects.equals(name, kWildcardName)) {
      if (out != null) {
        out.mnc = 0;
      }
      return true;
    }

    Matcher matcher = MNC_PATTERN.matcher(name);
    if (matcher.matches()) {
      out.mnc = Integer.parseInt(matcher.group(1));
      if (out.mnc == 0) {
        out.mnc = ACONFIGURATION_MNC_ZERO;
      }
      return true;
    }
    return false;
  }

  private static boolean parseMcc(final String name, ResTable_config out) {
    if (Objects.equals(name, kWildcardName)) {
      if (out != null) {
        out.mcc = 0;
      }
      return true;
    }

    Matcher matcher = MCC_PATTERN.matcher(name);
    if (matcher.matches()) {
      out.mcc = Integer.parseInt(matcher.group(1));
      return true;
    }
    return false;
  }

  // transliterated from https://android.googlesource.com/platform/frameworks/base/+/android-7.1.1_r13/tools/aapt/AaptConfig.cpp
  private void applyVersionForCompatibility(ResTable_config config) {
    if (config == null) {
      return;
    }
    int minSdk = 0;
    if (isTruthy(config.screenLayout2 & ResTable_config.MASK_SCREENROUND)) {
      minSdk = SDK_MNC;
    } else if (config.density == ResTable_config.DENSITY_ANY) {
      minSdk = SDK_LOLLIPOP;
    } else if (config.smallestScreenWidthDp != ResTable_config.SCREENWIDTH_ANY
        || config.screenWidthDp != ResTable_config.SCREENWIDTH_ANY
        || config.screenHeightDp != ResTable_config.SCREENHEIGHT_ANY) {
      minSdk = SDK_HONEYCOMB_MR2;
    } else if ((config.uiMode & ResTable_config.MASK_UI_MODE_TYPE)
        != ResTable_config.UI_MODE_TYPE_ANY
        ||  (config.uiMode & ResTable_config.MASK_UI_MODE_NIGHT)
        != ResTable_config.UI_MODE_NIGHT_ANY) {
      minSdk = SDK_FROYO;
    } else if ((config.screenLayout & ResTable_config.MASK_SCREENSIZE)
        != ResTable_config.SCREENSIZE_ANY
        ||  (config.screenLayout & ResTable_config.MASK_SCREENLONG)
        != ResTable_config.SCREENLONG_ANY
        || config.density != ResTable_config.DENSITY_DEFAULT) {
      minSdk = SDK_DONUT;
    }
    if (minSdk > config.sdkVersion) {
      config.sdkVersion = minSdk;
    }
  }
}
