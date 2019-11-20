//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.10.14 at 01:48:37 PM BST 
//


package org.linguafranca.pwdb.kdbx.jaxb.binding;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * This is where the values of the database are actually stored. You can have String valued
 *                 fields and you can have Binary valued fields. There are "Default" String fields (username and so on) and
 *                 there are custom string fields (custom only in that their names are not the names of default string
 *                 fields. Not really clear whether the keys are case sensitive.
 *             
 * 
 * <p>Java class for stringField complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="stringField"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Key" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="Value"&gt;
 *           &lt;complexType&gt;
 *             &lt;simpleContent&gt;
 *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *                 &lt;attribute name="Protected" type="{}keepassBoolean" /&gt;
 *                 &lt;attribute name="ProtectInMemory" type="{}keepassBoolean" /&gt;
 *               &lt;/extension&gt;
 *             &lt;/simpleContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "stringField", propOrder = {
    "key",
    "value"
})
public class StringField {

    @XmlElement(name = "Key", required = true)
    protected String key;
    @XmlElement(name = "Value", required = true)
    protected StringField.Value value;
    private transient StringField.Value pushedValue;

    /**
     * Gets the value of the key property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the value of the key property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setKey(String value) {
        this.key = value;
    }

    /**
     * Gets the value of the value property.
     * 
     * @return
     *     possible object is
     *     {@link StringField.Value }
     *     
     */
    public StringField.Value getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link StringField.Value }
     *     
     */
    public void setValue(StringField.Value value) {
        this.value = value;
    }


    public void pushValue() {
        pushedValue = value;
        this.value = new StringField.Value();
        this.value.setProtected(pushedValue.getProtected());
        this.value.setProtectInMemory(pushedValue.getProtectInMemory());
        this.value.setValue(pushedValue.getValue());
    }
    public void popValue() {
        if(this.pushedValue != null) {
            this.value = pushedValue;
            this.pushedValue = null;
        }
    }

    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;simpleContent&gt;
     *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
     *       &lt;attribute name="Protected" type="{}keepassBoolean" /&gt;
     *       &lt;attribute name="ProtectInMemory" type="{}keepassBoolean" /&gt;
     *     &lt;/extension&gt;
     *   &lt;/simpleContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "value"
    })
    public static class Value {

        @XmlValue
        protected String value;
        @XmlAttribute(name = "Protected")
        @XmlJavaTypeAdapter(Adapter2 .class)
        protected Boolean _protected;
        @XmlAttribute(name = "ProtectInMemory")
        @XmlJavaTypeAdapter(Adapter2 .class)
        protected Boolean protectInMemory;

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

        /**
         * Gets the value of the protected property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public Boolean getProtected() {
            return _protected;
        }

        /**
         * Sets the value of the protected property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setProtected(Boolean value) {
            this._protected = value;
        }

        /**
         * Gets the value of the protectInMemory property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public Boolean getProtectInMemory() {
            return protectInMemory;
        }

        /**
         * Sets the value of the protectInMemory property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setProtectInMemory(Boolean value) {
            this.protectInMemory = value;
        }

    }

}
