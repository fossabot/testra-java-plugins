package tech.testra.jvm.commons;

import java.io.Serializable;


/**
 * <p>Java class for Label complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Label"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="value" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 *
 */

public class Label implements Serializable
{

  private final static long serialVersionUID = 1L;

  protected String name;

  protected String value;

  /**
   * Gets the value of the name property.
   *
   * @return
   *     possible object is
   *     {@link String }
   *
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the value of the name property.
   *
   * @param value
   *     allowed object is
   *     {@link String }
   *
   */
  public void setName(String value) {
    this.name = value;
  }

  /**
   * Gets the value of the value property.
   *
   * @return
   *     possible object is
   *     {@link String }
   *
   */
  public String getValue() {
    return value;
  }

  /**
   * Sets the value of the value property.
   *
   * @param value
   *     allowed object is
   *     {@link String }
   *
   */
  public void setValue(String value) {
    this.value = value;
  }

  public Label withName(String value) {
    setName(value);
    return this;
  }

  public Label withValue(String value) {
    setValue(value);
    return this;
  }

}
