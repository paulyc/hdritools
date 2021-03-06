/*============================================================================
  HDRITools - High Dynamic Range Image Tools
  Copyright 2008-2013 Program of Computer Graphics, Cornell University

  Distributed under the OSI-approved MIT License (the "License");
  see accompanying file LICENSE for details.

  This software is distributed WITHOUT ANY WARRANTY; without even the
  implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  See the License for more information.
 -----------------------------------------------------------------------------
 Primary author:
     Edgar Velazquez-Armendariz <cs#cornell#edu - eva5>
============================================================================*/

///////////////////////////////////////////////////////////////////////////
//
// Copyright (c) 2004, Industrial Light & Magic, a division of Lucas
// Digital Ltd. LLC
//
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
// * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
// * Neither the name of Industrial Light & Magic nor the names of
// its contributors may be used to endorse or promote products derived
// from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
///////////////////////////////////////////////////////////////////////////

package edu.cornell.graphics.exr;

import edu.cornell.graphics.exr.ChannelList.ChannelListElement;
import edu.cornell.graphics.exr.attributes.Attribute;
import edu.cornell.graphics.exr.attributes.AttributeFactory;
import edu.cornell.graphics.exr.attributes.Box2iAttribute;
import edu.cornell.graphics.exr.attributes.ChannelListAttribute;
import edu.cornell.graphics.exr.attributes.CompressionAttribute;
import edu.cornell.graphics.exr.attributes.FloatAttribute;
import edu.cornell.graphics.exr.attributes.LineOrderAttribute;
import edu.cornell.graphics.exr.attributes.OpaqueAttribute;
import edu.cornell.graphics.exr.attributes.PreviewImageAttribute;
import edu.cornell.graphics.exr.attributes.TileDescriptionAttribute;
import edu.cornell.graphics.exr.attributes.TypedAttribute;
import edu.cornell.graphics.exr.attributes.V2fAttribute;
import edu.cornell.graphics.exr.ilmbaseto.Box2;
import edu.cornell.graphics.exr.ilmbaseto.Vector2;
import edu.cornell.graphics.exr.io.EXRByteArrayOutputStream;
import edu.cornell.graphics.exr.io.XdrInput;
import edu.cornell.graphics.exr.io.XdrOutput;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Abstraction of the header describing an OpenEXR file.
 * 
 * <p>The header contains the set of attributes and channels; it is used for
 * writing new files and contains all the information from existing ones.
 * An instance of {@code Header} provides iterator access to the named
 * attributes. While all aspects of a file are specified as attributes, this
 * class provides accessors and setters for the attributes which have to be
 * present in all OpenEXR files.</p>
 * 
 * <p>This class is modeled after {@code ImfHeader} in the original C++ OpenEXR
 * library.</p>
 * 
 * @since OpenEXR-JNI 2.1
 */
public final class Header implements Iterable<Entry<String, Attribute>> {
    
    private final static HashSet<String> predefinedAttributes;
    
    private final TreeMap<String, Attribute> map = new TreeMap<>();
    
    private static final AttributeFactory factory =
            AttributeFactory.newDefaultFactory();
    
    static {
        predefinedAttributes = new HashSet<>(8);
        predefinedAttributes.add("displayWindow");
        predefinedAttributes.add("dataWindow");
        predefinedAttributes.add("pixelAspectRatio");
        predefinedAttributes.add("screenWindowCenter");
        predefinedAttributes.add("screenWindowWidth");
        predefinedAttributes.add("lineOrder");
        predefinedAttributes.add("compression");
        predefinedAttributes.add("channels");
    }

    /**
     * Returns a read-only iterator over the existing attributes.
     * 
     * <p>The elements are ordered by attribute name. The returned entries
     * represent snapshots of the attributes at the time they were produced.
     * 
     * @return Returns an iterator over the existing attributes.
     */
    @Override
    public Iterator<Entry<String, Attribute>> iterator() {
        return Collections.unmodifiableSet(map.entrySet()).iterator();
    }
    
    /**
     * Returns a read-only {@link Set} view of the attribute names contained
     * in this header. The set's iterator returns the attribute names in
     * ascending order.
     * 
     * @return a read-only set view of the attribute names contained
     *         in this header.
     */
    public Set<String> attributeNameSet() {
        return Collections.unmodifiableSet(map.keySet());
    }
    
    /**
     * Returns a read-only {@link Set} view of the name-attribute mappings 
     * contained in this header. The set's iterator returns the entries in 
     * ascending name order.
     * 
     * @return a read-only set view of the name-attribute mappings 
     *         contained in this header.
     */
    public Set<Entry<String, Attribute>> entrySet() {
        return Collections.unmodifiableSet(map.entrySet());
    }
    
    /**
     * Returns the number of name-attribute mappings in this header.
     *
     * @return the number of name-attribute mappings in this header
     */
    public int size() {
        return map.size();
    }
    
    /**
     * Returns {@code true} if this header contains no name-attribute mappings.
     *
     * <p>This implementation returns {@code size() == 0}.</p>
     * 
     * @return {@code true} if this header contains no name-attribute mappings
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }
    
    /**
     * Default constructor. Creates a file with width and height 64, with the
     * attributes set as in {@link #Header(int, int) }.
     */
    public Header() {
        this(64, 64);
    }
    
    /**
     * Creates a header where the display window and the data window are both
     * set to <tt>[0,0] x [width-1, height-1]</tt>, with an empty channel list.
     * 
     * <p>The other predefined attributes are initialized as follows:
     * <table summary="Default values of predefined attributes">
     *   <tr><th>Attribute</th><th>Value</th></tr>
     *   <tr><td><tt>pixelAspectRatio</tt></td><td>1.0</td>
     *   <tr><td><tt>screenWindowCenter</tt></td><td>(0.0, 0.0)</td>
     *   <tr><td><tt>screenWindowWidth</tt></td><td>1.0</td>
     *   <tr><td><tt>lineOrder</tt></td><td>{@link LineOrder#INCREASING_Y}</td>
     *   <tr><td><tt>compression</tt></td><td>{@link Compression#ZIP}</td>
     * </table>
     * 
     * @param width positive width of the file.
     * @param height positive height of the file.
     * @throws IllegalArgumentException if either parameter is less than 1.
     */
    public Header(int width, int height) throws IllegalArgumentException {
        if (width < 1) {
            throw new IllegalArgumentException("Illegal width: "  + width);
        } else if (height < 1) {
            throw new IllegalArgumentException("Illegal height: " + height);
        }
        final int bW = width-1, bH = height-1;
        insert("displayWindow", new Box2iAttribute(new Box2<>(0, 0, bW, bH)));
        insert("dataWindow",    new Box2iAttribute(new Box2<>(0, 0, bW, bH)));
        insert("pixelAspectRatio",   new FloatAttribute(1.0f));
        insert("screenWindowCenter", new V2fAttribute(new Vector2<>(0.f, 0.f)));
        insert("screenWindowWidth",  new FloatAttribute(1.0f));
        insert("lineOrder",   new LineOrderAttribute(LineOrder.INCREASING_Y));
        insert("compression", new CompressionAttribute(Compression.ZIP));
        insert("channels", new ChannelListAttribute(new ChannelList()));
    }
    
    /**
     * Copy constructor. Creates a deep copy of the other header attributes.
     * 
     * @param other the header to duplicate.
     * @throws IllegalArgumentException if {@code other} is {@code null}.
     */
    public Header(Header other) throws IllegalArgumentException {
        if (other == null) {
            throw new IllegalArgumentException("null header");
        }
        for (Entry<String, Attribute> attr : other) {
            assert attr.getKey() != null && !attr.getKey().isEmpty();
            assert attr.getValue() != null;
            insert(attr.getKey(), attr.getValue().clone());
        }
    }
    
    /**
     * If an OpenEXR file contains any attribute names, attribute type names
     * or channel names longer than 31 characters, then the file cannot be
     * read by older versions of the IlmImf library (up to OpenEXR 1.6.1).
     * Before writing the file header, we check if the header contains
     * any names longer than 31 characters; if it does, then we set the
     * LONG_NAMES_FLAG in the file version number.  Older versions of the
     * IlmImf library will refuse to read files that have the LONG_NAMES_FLAG
     * set.  Without the flag, older versions of the library would mis-
     * interpret the file as broken.
     */
    private boolean usesLongNames() {
        for (Entry<String, Attribute> attr : this) {
            if (attr.getKey().length() >= 32 ||
                attr.getValue().typeName().length() >= 32) {
                return true;
            }
        }
        
        for (ChannelListElement c : getChannels()) {
            if (c.getName().length() >= 32) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Returns the appropriate version number that corresponds to this header.
     * @return the appropriate version number that corresponds to this header.
     */
    public int version() {
        int version = EXRVersion.EXR_VERSION;
        if (hasTileDescription()) {
            version |= EXRVersion.TILED_FLAG;
        }
        if (usesLongNames()) {
            version |= EXRVersion.LONG_NAMES_FLAG;
        }
        return version;
    }
    
    /**
     * Add an attribute to the header. If no attribute with name {@code n}
     * exists, a new attribute with name {@code n} and the same type as
     * {@code attr}, is added, and the value of {@code attr} is copied into
     * the new attribute.
     * 
     * <p>If an attribute with name {@code n} exists, and its type is the same
     * as {@code attr}, the value of {@code attr} is copied into this attribute.
     * </p>
     * <p>If an attribute with name {@code n} exists, and its type is different
     * from {@code attr} an {@code EXRTypeException} is thrown.
     *
     * @param n the non-empty name of the attribute.
     * @param attr the non-null attribute to add.
     * @throws IllegalArgumentException if {@code n} or {@code attr} are empty
     *         or {@code null}.
     * @throws EXRTypeException if an attribute with name {@code n} exists, and
     *         its type is different from {@code attr}.
     */
    public void insert(String n, Attribute attr) throws
            IllegalArgumentException, EXRTypeException {
        if (n == null || n.isEmpty()) {
            throw new IllegalArgumentException("Image attribute name cannot be "
                    + " an empty string.");
        } else if (attr == null) {
            throw new IllegalArgumentException("The attribute cannot be null");
        } else if (attr instanceof TypedAttribute && 
                ((TypedAttribute<?>)attr).getValue() == null) {
            throw new IllegalArgumentException("The typed attribute value " +
                    "cannot be null");
        }
        
        Attribute oldValue = map.get(n);
        if (oldValue != null && !oldValue.typeName().equals(attr.typeName())) {
            throw new EXRTypeException(String.format("Cannot assign a value of "
                    + "type \"%s\" to image attribute \"%s\" of type \"%s\".",
                    attr.typeName(), n, oldValue.typeName()));
        }
        Attribute myAttr = attr.clone();
        map.put(n, myAttr);
    }
    
    /**
     * If an attribute with the given name exists, then it is removed from the
     * map of present attributes. Otherwise this function is a "no-op".
     * 
     * @param name the name of the attribute to remove.
     * @throws IllegalArgumentException if {@code name} is either {@code null}
     *         or empty, or it corresponds to a predefined attribute.
     */
    public void erase(String name) throws IllegalArgumentException {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("invalid attribute name");
        } else if (predefinedAttributes.contains(name)) {
            throw new IllegalArgumentException("cannot erase predefined " +
                    "attribute: " + name);
        }
        map.remove(name);
    }
    
    /**
     * Returns a reference to the typed attribute with name {@code n} and value
     * type {@code T}. If no attribute with name {@code n} exists, an
     * {@code IllegalArgumentException} is thrown. If an attribute with name
     * {@code n} exists, but its value type is not {@code T}, an
     * {@code EXRTypeException} is thrown.
     * 
     * <p><b>Type limitations:</b> because Java implements generics via type
     * erasure, {@code Foo<T1>} and {@code Foo<T2>} have the same type; the
     * parameter types {@code T1} and {@code T2} may only be retrieved for
     * concrete instances. To avoid very convoluted type checking via reflection
     * this implementation actually checks that the <em>typed attribute</em>
     * class itself matches that of the existing attribute. Although this
     * technically violates the method description, in practice concrete
     * {@code TypedAttribute} instances are not parameterized and there is 
     * exactly one {@code TypedAttribute} class with value type {@code T}.</p>
     * 
     * @param <T> the underlying value type of the attribute.
     * @param name non-empty name of the desired attribute.
     * @param cls the class of the concrete {@code TypedAttribute<T>}
     *         implementation.
     * @return the typed attribute with name {@code n} and value type {@code T}.
     * @throws EXRTypeException if an attribute with name {@code n} exists, but
     *         its value type is not {@code T}.
     * @throws IllegalArgumentException if no attribute with name
     *         {@code n} exists.
     * @throws NullPointerException if either {@code name} or {@code cls}
     *         is {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> TypedAttribute<T> getTypedAttribute(String name,
            Class<? extends TypedAttribute<T>> cls) throws
            IllegalArgumentException, EXRTypeException {
        name = Objects.requireNonNull(name);
        cls  = Objects.requireNonNull(cls);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Image attribute name cannot be "
                    + " an empty string.");
        }
        Attribute attr = map.get(name);
        if (attr == null) {
            throw new IllegalArgumentException("Attribute not found: " + name);
        }
        if (!(attr instanceof TypedAttribute)) {
            throw new EXRTypeException("Not a typed attribute");
        }
        
        TypedAttribute<?> typedAttr = (TypedAttribute<?>) attr;
        if (cls != typedAttr.getClass()) {
            throw new EXRTypeException("The attribute does not match the " +
                    "requested type: expected: " +
                    cls.getCanonicalName() + ", actual: " +
                    typedAttr.getClass().getCanonicalName());
        }
        return (TypedAttribute<T>) typedAttr;
    }
    
    /**
     * Returns a reference to the typed attribute with name {@code n} and value
     * type {@code T}, or {@code null} if no attribute with name {@code n}
     * <em>and</em> type {@code T} exists.
     * 
     * <p><b>Type limitations:</b> because Java implements generics via type
     * erasure, {@code Foo<T1>} and {@code Foo<T2>} have the same type; the
     * parameter types {@code T1} and {@code T2} may only be retrieved for
     * concrete instances. To avoid very convoluted type checking via reflection
     * this implementation actually checks that the <em>typed attribute</em>
     * class itself matches that of the existing attributes. Although this
     * technically violates the method description, in practice concrete
     * {@code TypedAttribute} instances are not parameterized and there is 
     * exactly one {@code TypedAttribute} class with value type {@code T}.</p>
     * 
     * @param <T> the underlying value type of the attribute.
     * @param name non-empty name of the desired attribute.
     * @param cls the class of the concrete {@code TypedAttribute<T>}
     *         implementation.
     * @return the typed attribute with name {@code n} and value type {@code T}
     *         or {@code null}.
     * @throws NullPointerException if either {@code name} or {@code cls}
     *         is {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> TypedAttribute<T> findTypedAttribute(String name,
            Class<? extends TypedAttribute<T>> cls) {
        name = Objects.requireNonNull(name);
        cls  = Objects.requireNonNull(cls);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Image attribute name cannot be "
                    + " an empty string.");
        }
        Attribute attr = map.get(name);
        if ((attr == null) || (cls != attr.getClass())) {
            return null;
        } else {
            return (TypedAttribute<T>) attr;
        }
    }

    //--------------------------------
    // Access to predefined attributes
    //--------------------------------
    
    /**
     * Returns a reference to the value of the <tt>displayWindow</tt>
     * predefined attribute.
     * 
     * @return a reference to the value of the <tt>displayWindow</tt> attribute
     */
    public Box2<Integer> getDisplayWindow() {
        return getTypedAttribute("displayWindow",
                Box2iAttribute.class).getValue();
    }
    
    /**
     * Sets the value of the <tt>displayWindow</tt> predefined attribute
     * to {@code value}. This setter method does a deep copy of {@code value},
     * it does <em>not</em> copy a reference to it.
     * 
     * @param value a non-null {@code Box<Integer>}
     */
    public void setDisplayWindow(Box2<Integer> value) {
        final Box2<Integer> dw = getDisplayWindow();
        dw.xMin = value.xMin;
        dw.yMin = value.yMin;
        dw.xMax = value.xMax;
        dw.yMax = value.yMax;
    }
    
    /**
     * Sets the value of the <tt>displayWindow</tt> predefined attribute
     * to the box {@code [xMin,yMin]x[xMax,yMax]}.
     * 
     * @param xMin minimum {@literal x} value of the box
     * @param yMin minimum {@literal y} value of the box
     * @param xMax maximum {@literal x} value of the box
     * @param yMax maximum {@literal y} value of the box
     */
    public void setDisplayWindow(int xMin, int yMin, int xMax, int yMax) {
        final Box2<Integer> dw = getDisplayWindow();
        dw.xMin = xMin;
        dw.yMin = yMin;
        dw.xMax = xMax;
        dw.yMax = yMax;
    }
    
    /**
     * Returns a reference to the value of the <tt>dataWindow</tt>
     * predefined attribute.
     * 
     * @return a reference to the value of the <tt>dataWindow</tt> attribute
     */
    public Box2<Integer> getDataWindow() {
        return getTypedAttribute("dataWindow",
                Box2iAttribute.class).getValue();
    }
    
    /**
     * Sets the value of the <tt>dataWindow</tt> predefined attribute
     * to {@code value}. This setter method does a deep copy of {@code value},
     * it does <em>not</em> copy a reference to it.
     * 
     * @param value a non-null {@code Box<Integer>}
     */
    public void setDataWindow(Box2<Integer> value) {
        final Box2<Integer> dw = getDataWindow();
        dw.xMin = value.xMin;
        dw.yMin = value.yMin;
        dw.xMax = value.xMax;
        dw.yMax = value.yMax;
    }
    
    /**
     * Sets the value of the <tt>dataWindow</tt> predefined attribute
     * to the box {@code [xMin,yMin]x[xMax,yMax]}.
     * 
     * @param xMin minimum {@literal x} value of the box
     * @param yMin minimum {@literal y} value of the box
     * @param xMax maximum {@literal x} value of the box
     * @param yMax maximum {@literal y} value of the box
     */
    public void setDataWindow(int xMin, int yMin, int xMax, int yMax) {
        final Box2<Integer> dw = getDataWindow();
        dw.xMin = xMin;
        dw.yMin = yMin;
        dw.xMax = xMax;
        dw.yMax = yMax;
    }
    
    /**
     * Returns a reference to the value of the <tt>pixelAspectRatio</tt>
     * predefined attribute.
     * 
     * @return a reference to the value of the <tt>pixelAspectRatio</tt>
     * attribute
     */
    public Float getPixelAspectRatio() {
        return getTypedAttribute("pixelAspectRatio",
                FloatAttribute.class).getValue();
    }
    
    /**
     * Sets the value of the <tt>pixelAspectRatio</tt> predefined attribute
     * to {@code value}.
     * 
     * @param value a float greater than zero.
     */
    public void setPixelAspectRatio(float value) {
        getTypedAttribute("pixelAspectRatio",
                FloatAttribute.class).setValue(value);
    }
    
    /**
     * Returns a reference to the value of the <tt>screenWindowCenter</tt>
     * predefined attribute.
     * 
     * @return a reference to the value of the <tt>screenWindowCenter</tt>
     * attribute
     */
    public Vector2<Float> getScreenWindowCenter() {
        return getTypedAttribute("screenWindowCenter",
                V2fAttribute.class).getValue();
    }
    
    /**
     * Sets the value of the <tt>screenWindowCenter</tt> predefined attribute
     * to {@code value}. This setter method does a deep copy of {@code value},
     * it does <em>not</em> copy a reference to it.
     * 
     * @param value a non-null {@code Vector2<Float>}
     */
    public void setScreenWindowCenter(Vector2<Float> value) {
        final Vector2<Float> swc = getScreenWindowCenter();
        swc.x = value.x;
        swc.y = value.y;
    }
    
    /**
     * Sets the value of the <tt>screenWindowCenter</tt> predefined attribute
     * to the point {@code (x,y)}.
     * 
     * @param x the {@literal x} component of the point
     * @param y the {@literal y} component of the point
     */
    public void setScreenWindowCenter(float x, float y) {
        final Vector2<Float> swc = getScreenWindowCenter();
        swc.x = x;
        swc.y = y;
    }
    
    /**
     * Returns a reference to the value of the <tt>screenWindowWidth</tt>
     * predefined attribute.
     * 
     * @return a reference to the value of the <tt>screenWindowWidth</tt> 
     * attribute
     */
    public Float getScreenWindowWidth() {
        return getTypedAttribute("screenWindowWidth",
                FloatAttribute.class).getValue();
    }
    
    /**
     * Sets the value of the <tt>screenWindowWidth</tt> predefined attribute
     * to {@code value}.
     * 
     * @param value a float greater than zero.
     */
    public void setScreenWindowWidth(float value) {
        getTypedAttribute("screenWindowWidth",
                FloatAttribute.class).setValue(value);
    }
    
    /**
     * Returns a reference to the value of the <tt>channels</tt>
     * predefined attribute.
     * 
     * @return a reference to the value of the <tt>channels</tt> attribute
     */
    public ChannelList getChannels() {
        return getTypedAttribute("channels",
                ChannelListAttribute.class).getValue();
    }
    
    /**
     * Returns a reference to the value of the <tt>lineOrder</tt>
     * predefined attribute.
     * 
     * @return a reference to the value of the <tt>lineOrder</tt> attribute
     */
    public LineOrder getLineOrder() {
        return getTypedAttribute("lineOrder",
                LineOrderAttribute.class).getValue();
    }
    
    /**
     * Sets the value of the <tt>lineOrder</tt> predefined attribute
     * to {@code value}.
     * 
     * @param value a non-null {@code LineOrder}.
     */
    public void setLineOrder(LineOrder value) {
        getTypedAttribute("lineOrder",
                LineOrderAttribute.class).setValue(value);
    }
    
    /**
     * Returns a reference to the value of the <tt>compression</tt>
     * predefined attribute.
     * 
     * @return a reference to the value of the <tt>compression</tt> attribute
     */
    public Compression getCompression() {
        return getTypedAttribute("compression",
                CompressionAttribute.class).getValue();
    }
    
    /**
     * Sets the value of the <tt>compression</tt> predefined attribute
     * to {@code value}.
     * 
     * @param value a non-null {@code Compression}.
     */
    public void setCompression(Compression value) {
        getTypedAttribute("compression",
                CompressionAttribute.class).setValue(value);
    }
    
    /**
     * Returns a reference to the value of the <tt>tiles</tt> attribute
     * attribute if and only if {@link #hasTileDescription() } 
     * returns {@code true}.
     * 
     * <p>The "tiles" attribute must be present in any tiled image file.
     * When present, it describes various properties of the tiles that make up
     * the file. If the "tiles" attribute is not present this method
     * throws an exception.</p>
     * 
     * @return a reference to the value of the <tt>tiles</tt> attribute
     */
    public TileDescription getTileDescription() {
        return getTypedAttribute("tiles",
                TileDescriptionAttribute.class).getValue();
    }
    
    /**
     * Returns whether the header contains a {@link TileDescriptionAttribute}
     * whose name is "tiles".
     * 
     * <p>The "tiles" attribute must be present in any tiled image file.
     * When present, it describes various properties of the tiles that make up
     * the file. The implementation simply returns
     * {@code findTypedAttribute("tiles",TileDescriptionAttribute.class)!=null}.
     * </p>
     * 
     * @return {@code true} if the header contains a
     * {@link TileDescriptionAttribute} whose name is "tiles", {@code false}
     * otherwise.
     */
    public boolean hasTileDescription() {
        return findTypedAttribute("tiles",TileDescriptionAttribute.class)!=null;
    }
    
    /**
     * Examines the header, and throws an {@code IllegalArgumentException} if
     * the header is non-null and it finds something wrong (e.g. empty display
     * window, negative pixel aspect ratio, unknown compression scheme, etc.)
     *
     * @param h the header to verify
     * @param isTiled whether the header is supposed to have tiles or not
     * @throws IllegalArgumentException if there is something wrong
     *         with the file
     */
    public static void sanityCheck(Header h, boolean isTiled) throws
            IllegalArgumentException {
        if (h == null) {
            return;
        }
        //
        // The display window and the data window must each
        // contain at least one pixel. In addition, the
        // coordinates of the window corners must be small
        // enough to keep expressions like max-min+1 or
        // max+min from overflowing.
        //
        
        Box2<Integer> displayWindow = h.getDisplayWindow();
        if (displayWindow.xMin.compareTo(displayWindow.xMax) > 0 ||
            displayWindow.yMin.compareTo(displayWindow.yMax) > 0 ||
            displayWindow.xMin.compareTo(-Integer.MAX_VALUE / 2) <= 0 ||
            displayWindow.yMin.compareTo(-Integer.MAX_VALUE / 2) <= 0 ||
            displayWindow.xMax.compareTo( Integer.MAX_VALUE / 2) <= 0 ||
            displayWindow.yMax.compareTo( Integer.MAX_VALUE / 2) <= 0)
        {
            throw new IllegalArgumentException("Invalid display window "
                    + "in image header.");            
        }
        
        Box2<Integer> dataWindow = h.getDataWindow();
        if (dataWindow.xMin.compareTo(dataWindow.xMax) > 0 ||
            dataWindow.yMin.compareTo(dataWindow.yMax) > 0 ||
            dataWindow.xMin.compareTo(-Integer.MAX_VALUE / 2) <= 0 ||
            dataWindow.yMin.compareTo(-Integer.MAX_VALUE / 2) <= 0 ||
            dataWindow.xMax.compareTo( Integer.MAX_VALUE / 2) <= 0 ||
            dataWindow.yMax.compareTo( Integer.MAX_VALUE / 2) <= 0)
        {
            throw new IllegalArgumentException("Invalid data window "
                    + "in image header.");            
        }
        
        //
        // The pixel aspect ratio must be greater than 0.
        // In applications, numbers like the the display or
        // data window dimensions are likely to be multiplied
        // or divided by the pixel aspect ratio; to avoid
        // arithmetic exceptions, we limit the pixel aspect
        // ratio to a range that is smaller than theoretically
        // possible (real aspect ratios are likely to be close
        // to 1.0 anyway).
        //
        
        float pixelAspectRatio = h.getPixelAspectRatio();
        
        final float MIN_PIXEL_ASPECT_RATIO = 1e-6f;
        final float MAX_PIXEL_ASPECT_RATIO = 1e+6f;

        if (pixelAspectRatio < MIN_PIXEL_ASPECT_RATIO ||
            pixelAspectRatio > MAX_PIXEL_ASPECT_RATIO) {
            throw new IllegalArgumentException("Invalid pixel aspect ratio "
                    + "in image header.");
        }
        
        //
        // The screen window width must not be less than 0.
        // The size of the screen window can vary over a wide
        // range (fish-eye lens to astronomical telescope),
        // so we can't limit the screen window width to a
        // small range.
        //
        
        float screenWindowWidth = h.getScreenWindowWidth();
        if (screenWindowWidth < 0.0f) {
            throw new IllegalArgumentException("Invalid screen window width "
                    + "in image header.");
        }
        
        //
        // If the file is tiled, verify that the tile description has resonable
        // values and check to see if the lineOrder is one of the predefined 3.
        // If the file is not tiled, then the lineOrder can only be INCREASING_Y
        // or DECREASING_Y.
        //
        
        LineOrder lineOrder = h.getLineOrder();
        if (isTiled) {
            if (!h.hasTileDescription()) {
                throw new IllegalArgumentException("Tiled image has no tile "
                        + "description attribute.");
            }
            TileDescription tileDesc = h.getTileDescription();
            if (tileDesc.xSize <= 0 || tileDesc.ySize <= 0) {
                throw new IllegalArgumentException("Invalid tile size in "
                        + "image header.");
            }
            if (tileDesc.mode == null) {
                throw new IllegalArgumentException("Invalid level mode in "
                        + "image header.");
            }
            if (tileDesc.roundingMode == null) {
                throw new IllegalArgumentException("Invalid rounding mode in "
                        + "image header.");
            }
            if (lineOrder == null) {
                throw new IllegalArgumentException("Invalid line order in "
                        + "image header.");
            }
        }
        else {
            if (lineOrder == null || 
                (!lineOrder.equals(LineOrder.INCREASING_Y) &&
                 !lineOrder.equals(LineOrder.INCREASING_Y))) {
                throw new IllegalArgumentException("Invalid line order in "
                        + "image header.");
            }
        }
        
        if (h.getCompression() == null) {
            throw new IllegalArgumentException("Invalid compression in "
                    + "image header.");
        }
       
        //
        // Check the channel list:
        //
        // If the file is tiled then for each channel, the type must be one of
        // the predefined values, and the x and y sampling must both be 1.
        //
        // If the file is not tiled then for each channel, the type must be one
        // of the predefined values, the x and y coordinates of the data
        // window's upper left corner must be divisible by the x and y
        // subsampling factors, and the width and height of the data window must
        // be divisible by the x and y subsampling factors.
        //
        
        ChannelList channels = h.getChannels();
        if (isTiled) {
            for (ChannelList.ChannelListElement elem : channels) {
                final String name = elem.getName();
                final Channel c   = elem.getChannel();
                if (c.type == null) {
                    throw new IllegalArgumentException("Pixel type of \"" +
                            name + "\" image channel is invalid.");
                }
                if (c.xSampling != 1) {
                    throw new IllegalArgumentException("The x subsampling "
                            + "factor for the \"" + name + "\" channel "
                            + "is not 1.");
                }
                if (c.ySampling != 1) {
                    throw new IllegalArgumentException("The y subsampling "
                            + "factor for the \"" + name + "\" channel "
                            + "is not 1.");
                }
            }
        }
        else {
            for (ChannelList.ChannelListElement elem : channels) {
                final String name = elem.getName();
                final Channel c   = elem.getChannel();
                if (c.type == null) {
                    throw new IllegalArgumentException("Pixel type of \"" +
                            name + "\" image channel is invalid.");
                }
                if (c.xSampling < 1) {
                    throw new IllegalArgumentException("The x subsampling "
                            + "factor for the \"" + name + "\" channel "
                            + "is invalid.");
                }
                if (c.ySampling < 1) {
                    throw new IllegalArgumentException("The y subsampling "
                            + "factor for the \"" + name + "\" channel "
                            + "is invalid.");
                }
                if (dataWindow.xMin.intValue() % c.xSampling != 0) {
                    throw new IllegalArgumentException("The minimum x "
                            + "coordinate of the image's data window is not a "
                            + "multiple of the x subsampling factor of the \"" 
                            + name + "\" channel.");
                }
                if (dataWindow.yMin.intValue() % c.ySampling != 0) {
                    throw new IllegalArgumentException("The minimum y "
                            + "coordinate of the image's data window is not a "
                            + "multiple of the y subsampling factor of the \"" 
                            + name + "\" channel.");
                }
                
                int width = dataWindow.xMax.intValue() -
                            dataWindow.xMin.intValue() + 1;
                if (width % c.xSampling != 0) {
                    throw new IllegalArgumentException("The number of pixels "
                            + "per row in the image's data window is not a "
                            + "multiple of the x subsampling factor of the \"" 
                            + name + "\" channel.");
                }
                
                int height = dataWindow.yMax.intValue() -
                             dataWindow.yMin.intValue() + 1;
                if (height % c.ySampling != 0) {
                    throw new IllegalArgumentException("The number of pixels "
                            + "per column in the image's data window is not a "
                            + "multiple of the y subsampling factor of the \"" 
                            + name + "\" channel.");
                }
            }
        }
    }

    /**
     * Populates this header by reading from the {@link XdrInput} encapsulating
     * an input stream, replacing its previous contents.
     * 
     * <p>This method reads a sequence of name-attribute pairs. The names are
     * null-terminated strings; a zero-length attribute name indicates the
     * end of the header. Each attribute is stored as a null-terminated string
     * with the attribute's type name, followed by a 32-bit integer with the
     * length of the attribute's data, followed by that many bytes.</p>
     * 
     * <p>Depending on the {@code version} value the will be specific limitations
     * on the attribute values and structure of the header.</p>
     * 
     * @param input data source which contains the header data
     * @param version OpenEXR version number as stored in the file
     * @throws EXRIOException if there is an error in the file format or
     *         if there is another I/O error
     */
    public void readFrom(XdrInput input, int version) throws 
            EXRIOException {
        map.clear();
        final int maxNameLength = EXRVersion.getMaxNameLength(version);
        
        // Read all attributes
        for(;;) {
            // Read the name of the attribute.
            // A zero-length attribute name indicates the end of the header.
            String name = input.readNullTerminatedUTF8(maxNameLength);
            if (name.isEmpty()) {
                break;
            }

            // Read the attribute type and the size of the attribute value.
            String type = input.readNullTerminatedUTF8(maxNameLength);
            int size    = input.readInt();
            
            Attribute currentAttr = map.get(name);
            if (currentAttr != null) {
                // The attribute already exists (for example,
                // because it is a predefined attribute).
                // Read the attribute's new value from the file.
                if (!type.equals(currentAttr.typeName())) {
                    throw new EXRIOException("Unexpected type for "
                            + "image attribute \"" + name + "\".");
                }
                currentAttr.readValueFrom(input, size, version);
            }
            else {
                // The new attribute does not exist yet.
                // If the attribute type is of a known type,
                // read the attribute value. If the attribute
                // is of an unknown type, read its value and
                // store it as an OpaqueAttribute.
                Attribute attr;
                if (factory.isKnownType(type)) {
                    attr = factory.newAttribute(type);
                } else {
                    attr = new OpaqueAttribute(type);
                }
                attr.readValueFrom(input, size, version);
                insert(name, attr);
            }
        }
    }
    
    /**
     * Serializes this header by writing into the {@link XdrOutput}
     * encapsulating an output stream.
     * 
     * <p>This method writes a sequence of name-attribute pairs. The names are
     * null-terminated strings; a zero-length attribute name marks the
     * end of the header. Each attribute is stored as a null-terminated string
     * with the attribute's type name, followed by a 32-bit integer with the
     * length of the attribute's data, followed by that many bytes.</p>
     * 
     * <p>This method does <em>not</em> write neither the version number nor
     * the magic number, it serializes the header data only.</p>
     * 
     * <p>If the header contains a preview image attribute,
     * then {@code writeTo()} returns the position of that attribute in the
     * output stream; this information is used to update the preview image
     * when writing an output file. If the header contains no preview image
     * attribute, then {@code writeTo()} returns {@literal 0}.</p>
     * 
     * @param output data sink where the header data will be written
     * @return the position of the preview image attribute in the output
     *         stream, or {@literal 0} if the header contains no such attribute
     * @throws EXRIOException if there is an I/O error
     * @see #version() 
     */
    public long writeTo(XdrOutput output) throws EXRIOException {
        // Write all attributes.  If we have a preview image attribute,
        // keep track of its position in the file.
        long previewPosition = 0L;
        
        final int version = version();
        final int maxNameLength = EXRVersion.getMaxNameLength(version);
        TypedAttribute<PreviewImage> preview = findTypedAttribute("preview",
                PreviewImageAttribute.class);
        
        EXRByteArrayOutputStream attrStream = new EXRByteArrayOutputStream();
        XdrOutput attrOutput = new XdrOutput(attrStream);
        
        for (Entry<String, Attribute> attr : this) {
            // Write the attribute's name and type.
            final Attribute value = attr.getValue();
            output.writeNullTerminatedUTF8(attr.getKey(),    maxNameLength);
            output.writeNullTerminatedUTF8(value.typeName(), maxNameLength);
            
            // Write the size of the attribute value, and the value itself.
            attrOutput.position(0);
            value.writeValueTo(attrOutput, version);
            final int valueSize = (int) attrOutput.position();
            output.writeInt(valueSize);
            
            if (value.equals(preview)) {
                previewPosition = output.position();
            }
            
            output.writeByteArray(attrStream.array(), 0, valueSize);
        }
        
        // Write zero-length attribute name to mark the end of the header.
        output.writeNullTerminatedUTF8("");
        
        return previewPosition;
    }

    /**
     * Returns the hash code value for this header.  The hash code of a header
     * is defined to be the sum of the hash codes of each attribute in the
     * header's {@code entrySet()} view. This ensures that {@code h1.equals(h2)}
     * implies that {@code h1.hashCode()==h2.hashCode()} for any two headers
     * h1 and h2, as required by the general contract of
     * {@code Object.hashCode}.
     * 
     * <p>This implementation relies on the underlying map, which iterates over
     * {@code entrySet()}, calling {@code hashCode()} on each attribute (entry)
     * in the set, and adding up the results.</p>
     *  
     * @return the hash code value for this header.
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + this.map.hashCode();
        return hash;
    }

    /**
     * Compares the specified object with this header for equality.  Returns
     * <tt>true</tt> if the given object is also a header and the two headers
     * contain the same attributes.  More formally, two headers <tt>h1</tt> and
     * <tt>h2</tt> represent the same attributes if
     * <tt>h1.entrySet().equals(h2.entrySet())</tt>.  This ensures that the
     * <tt>equals</tt> method works properly across different implementations
     * of the <tt>Map</tt> interface.
     *
     * <p>This implementation first checks if the specified object is this 
     * header; if so it returns <tt>true</tt>.  Then, it checks if the specified
     * object is a header whose size is identical to the size of this header; if
     * not, it returns <tt>false</tt>.  If so, it iterates over this header's
     * <tt>entrySet</tt> collection, and checks that the specified header
     * contains each mapping that this header contains.  If the specified header
     * fails to contain such a mapping, <tt>false</tt> is returned.  If the
     * iteration completes, <tt>true</tt> is returned.
     *
     * @param obj object to be compared for equality with this header
     * @return <tt>true</tt> if the specified object is equal to this header
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Header other = (Header) obj;
        if (!Objects.equals(this.map, other.map)) {
            return false;
        }
        return true;
    }

}
