/*
 *
 * Copyright (c) 2008-2020 by Rodney Kinney, Brent Easton
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License (LGPL) as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, copies are available
 * at http://www.opensource.org.
 */
package VASSAL.configure;

import VASSAL.tools.NamedKeyManager;
import VASSAL.tools.NamedKeyStroke;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import net.miginfocom.swing.MigLayout;

public class NamedHotKeyConfigurer extends Configurer {
  private final JTextField keyStroke = new JTextField(16);
  private final JTextField keyName = new JTextField(16);
  private JPanel controls;

  public static String getFancyString(NamedKeyStroke k) {
    String s = getString(k);
    if (s.length() > 0) {
      s = "[" + s + "]";
    }
    return s;
  }

  public static String getString(NamedKeyStroke k) {
    return (k == null || k.isNull()) ? "" : getString(k.getStroke());
  }

  public static String getString(KeyStroke k) {
    return NamedKeyManager.isNamed(k) ? "" : HotKeyConfigurer.getString(k);
  }

  public NamedHotKeyConfigurer(String key, String name) {
    this(key, name, new NamedKeyStroke());
  }

  public NamedHotKeyConfigurer(String key, String name, NamedKeyStroke val) {
    super(key, name, val);
  }

  public NamedHotKeyConfigurer(NamedKeyStroke val) {
    this(null, null, val);
  }

  @Override
  public String getValueString() {
    return encode((NamedKeyStroke) getValue());
  }

  public NamedKeyStroke getValueNamedKeyStroke() {
    return (NamedKeyStroke) value;
  }

  public boolean isNamed() {
    return value != null && ((NamedKeyStroke) value).isNamed();
  }

  @Override
  public void setValue(Object o) {
    super.setValue(o);
    setFrozen(true); // Prevent changes to the input fields triggering further updates
    if (controls != null) {
      if (isNamed()) {
        keyStroke.setText("");
      }
      else {
        keyName.setText("");
        keyStroke.setText(keyToString());
      }
    }
    setFrozen(false);
  }

  @Override
  public void setValue(String s) {
    setValue(s == null ? null : decode(s));
  }

  private void updateValueFromKeyName() {
    if (! isFrozen()) {
      final String key = keyName.getText();
      if (key.isEmpty()) {
        setValue(NamedKeyStroke.NULL_KEYSTROKE);
      }
      else {
        setValue(new NamedKeyStroke(NamedKeyManager.getMarkerKeyStroke(), key));
      }
    }
  }

  @Override
  public Component getControls() {
    if (controls == null) {
      controls = new ConfigurerPanel(getName(), "[fill,grow]", "[][fill,grow]"); // NON-NLS

      keyStroke.setMaximumSize(new Dimension(keyStroke.getMaximumSize().width, keyStroke.getPreferredSize().height));
      keyStroke.setText(keyToString());
      keyStroke.addKeyListener(new KeyStrokeAdapter());
      ((AbstractDocument) keyStroke.getDocument()).setDocumentFilter(new KeyStrokeFilter());

      keyName.setMaximumSize(new Dimension(keyName.getMaximumSize().width, keyName.getPreferredSize().height));
      keyName.setText(getValueNamedKeyStroke() == null ? null : getValueNamedKeyStroke().getName());
      ((AbstractDocument) keyName.getDocument()).setDocumentFilter(new KeyNameFilter());

      final JPanel panel = new JPanel(new MigLayout("ins 0", "[fill,grow]0[]0[fill,grow]")); // NON-NLS
      panel.add(keyStroke, "grow"); // NON-NLS
      panel.add(new JLabel("-")); // NON-NLS
      panel.add(keyName, "grow"); // NON-NLS

      controls.add(panel, "grow"); // NON-NLS
    }
    return controls;
  }

  public String keyToString() {
    return getString((NamedKeyStroke) getValue());
  }

  protected boolean isPrintableAscii(char c) {
    return isPrintableAscii((int) c);
  }

  protected boolean isPrintableAscii(int i) {
    return i >= ' ' && i <= '~';
  }

  /**
   * Decode a String into a NamedKeyStroke
   */
  public static NamedKeyStroke decode(String s) {
    if (s == null) {
      return NamedKeyStroke.NULL_KEYSTROKE;
    }
    String[] parts = s.split(",");
    if (parts.length < 2) {
      return NamedKeyStroke.NULL_KEYSTROKE;
    }

    try {
      KeyStroke stroke = KeyStroke.getKeyStroke(
        Integer.parseInt(parts[0]),
        Integer.parseInt(parts[1])
      );
      String name = null;
      if (parts.length > 2) {
        name = parts[2];
      }
      return new NamedKeyStroke(stroke, name);
    }
    catch (Exception e) {
      return NamedKeyStroke.NULL_KEYSTROKE;
    }
  }

  /**
   * Encode a NamedKeyStroke into a String
   */
  public static String encode(NamedKeyStroke stroke) {
    if (stroke == null) {
      return "";
    }
    KeyStroke key = stroke.getStroke();
    if (key == null) {
      return "";
    }
    String s = key.getKeyCode() + "," + key.getModifiers();
    if (stroke.isNamed()) {
      s += "," + stroke.getName();
    }
    return s;
  }

  private class KeyStrokeAdapter extends KeyAdapter {
    @Override
    public void keyPressed(KeyEvent e) {
      switch (e.getKeyCode()) {
      case KeyEvent.VK_DELETE:
      case KeyEvent.VK_BACK_SPACE:
        setValue(NamedKeyStroke.NULL_KEYSTROKE);
        break;
      case KeyEvent.VK_SHIFT:
      case KeyEvent.VK_CONTROL:
      case KeyEvent.VK_META:
      case KeyEvent.VK_ALT:
        break;
      default:
        setValue(NamedKeyStroke.getKeyStrokeForEvent(e));
      }
    }

    @Override
    public void keyReleased(KeyEvent e) {
      keyStroke.setText(getString((NamedKeyStroke) getValue()));
    }
  }

  private class KeyNameFilter extends DocumentFilter {
    @Override
    public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
      super.remove(fb, offset, length);
      updateValueFromKeyName();
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
      super.insertString(fb, offset, string, attr);
      updateValueFromKeyName();
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
      super.replace(fb, offset, length, text, attrs);
      updateValueFromKeyName();
    }
  }

  private class KeyStrokeFilter extends DocumentFilter {
    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
      super.replace(fb, 0, keyStroke.getText().length(), text, attrs);
    }
  }
}
