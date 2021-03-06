/*============================================================================
  HDRITools - High Dynamic Range Image Tools
  Copyright 2008-2012 Program of Computer Graphics, Cornell University

  Distributed under the OSI-approved MIT License (the "License");
  see accompanying file LICENSE for details.

  This software is distributed WITHOUT ANY WARRANTY; without even the
  implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  See the License for more information.
 -----------------------------------------------------------------------------
 Primary author:
     Edgar Velazquez-Armendariz <cs#cornell#edu - eva5>
============================================================================*/

package edu.cornell.graphics.exr.attributes;

import edu.cornell.graphics.exr.EXRIOException;
import edu.cornell.graphics.exr.io.XdrInput;
import edu.cornell.graphics.exr.io.XdrOutput;
import java.util.Objects;

/**
 * Base class for attributes holding a value of the parameterized
 * class {@code T}.
 * 
 * @since OpenEXR-JNI 2.1
 */
public abstract class TypedAttribute<T> implements Attribute {
    
    /** Reference to the value of this attribute. */
    protected T value;  
        
    /**
     * Returns the enumeration constant {@code e} such that
     * {@code (e.ordinal() == ordinal)} or {@code null} if a match is not found.
     * 
     * <p>For simplicity this implementation does a linear search in the values
     * returned by {@code cls.getEnumConstants()}.</p>
     * 
     * @param <E> an enumeration class
     * @param ordinal desired ordinal to match
     * @param cls class of the desired enumeration
     * @return the enumeration constant {@code e} such that
     *         {@code (e.ordinal() == ordinal)} or {@code null}
     * @see Class#getEnumConstants() 
     */
    protected static <E extends Enum<E>> E valueOf(int ordinal, Class<E> cls) {
        E[] values = cls.getEnumConstants();
        for (E e : values) {
            if (e.ordinal() == ordinal) {
                return e;
            }
        }
        return null;
    }
    
    /**
     * Returns the enumeration constant {@code e} such that
     * {@code (e.ordinal() == ordinal)} or throws {@code EXRIOException} 
     * if a match is not found.
     * 
     * <p>For simplicity this implementation does a linear search in the values
     * returned by {@code cls.getEnumConstants()}.</p>
     * 
     * @param <E> an enumeration class
     * @param ordinal desired ordinal to match
     * @param cls class of the desired enumeration
     * @return the enumeration constant {@code e} such that
     *         {@code (e.ordinal() == ordinal)}
     * @throws EXRIOException if a matching enumeration constant is not found
     * @see Class#getEnumConstants() 
     */
    protected static <E extends Enum<E>> E checkedValueOf(int ordinal,
            Class<E> cls) throws EXRIOException {
        E e = valueOf(ordinal, cls);
        if (e != null) {
            return e;
        } else {
            throw new EXRIOException("Not a valid ordinal: " + ordinal);
        }
    }
    
    /**
     * Helper method that throws an {@code EXRIOException} if
     * {@code (expected != actual)}.
     * @param expected number of bytes expected
     * @param actual   number of bytes of the actual I/O operation
     * @throws EXRIOException if {@code (expected != actual)}
     */
    protected static void checkSize(int expected, int actual)
            throws EXRIOException {
        if (expected != actual) {
            throw new EXRIOException(String.format(
                    "Expected size %d, actual %d", expected, actual));
        }
    }
    
    /**
     * Default constructor which initializes {@code value} to {@code null}.
     */
    protected TypedAttribute() {
        // empty
    }
    
    /**
     * Initializes {@code this.value} by copying a reference to the parameter
     * {@code value}.
     * 
     * @param value reference to the initial value of this attribute
     * @throws NullPointerException if {@code value} is {@code null}.
     */
    protected TypedAttribute(T value) {
        this.value = Objects.requireNonNull(value, "value must not be null");
    }
    
    /**
     * Set the value of this attribute by reading from the given input buffer.
     * The {@code version} parameters is the 4-byte integer
     * following the magic number at the beginning of an OpenEXR file with the
     * file version and feature flags.
     * 
     * <p>The default implementation throws an
     * {@code UnsupportedOperationException}.</p>
     * 
     * @param input data input from which the value will be read.
     * @param version file version and flags as provided in the OpenEXR file.
     * @throws EXRIOException if there is an error in the file format
     *         or if there is an I/O error
     */
    protected void readValueFrom(XdrInput input, int version) 
            throws EXRIOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Set the value of this attribute by reading from the given input buffer.
     * The {@code size} parameter contains the size in bytes specified in the
     * header for the attribute's value; {@code version} is the 4-byte integer
     * following the magic number at the beginning of an OpenEXR file with the
     * file version and feature flags.
     * 
     * <p>The default implementation calls {@link #readValueFrom(XdrInput, int)}
     * and checks that the actual bytes consumed from {@code input} match those
     * specified by {@code size}, throwing an {@code EXRIOException} if that
     * is not the case.
     * 
     * @param input data input from which the value will be read.
     * @param size amount of bytes to be read according to the header.
     * @param version file version and flags as provided in the OpenEXR file.
     * @throws EXRIOException if there is an error in the file format or
     *         an I/O error.
     */
    @Override
    public void readValueFrom(XdrInput input, int size, int version)
            throws EXRIOException {
        final long p0  = input.position();
        readValueFrom(input, version);
        final int actualCount = (int) (input.position() - p0);
        checkSize(size, actualCount);
    }
    
    /**
     * Writes the value of this attribute into the given output buffer. This
     * method is used by the default implementation of
     * {@link #writeValueTo(XdrOutput, int) }.
     * 
     * <p>The default implementation throws an
     * {@code UnsupportedOperationException}.</p>
     * 
     * @param output data output into which the value will be written.
     * @throws EXRIOException if there is an I/O error.
     */
    protected void writeValueTo(XdrOutput output) throws EXRIOException {
        throw new UnsupportedOperationException("Not implemented");
    }
    
    /**
     * Writes the value of this attribute into the given output buffer.
     * The {@code version} parameter is the 4-byte integer following the
     * magic number at the beginning of an OpenEXR file with the
     * file version and feature flags.
     * 
     * <p>The default implementation simply calls
     * {@link #writeValueTo(XdrOutput) }.</p>
     * 
     * @param output data output into which the value will be written.
     * @param version file version and flags as provided in the OpenEXR file.
     * @throws EXRIOException if there is an I/O error.
     */
    @Override
    public void writeValueTo(XdrOutput output, int version)
            throws EXRIOException {
        writeValueTo(output);
    }
    
    /**
     * Returns a reference to the current value of this attribute instance.
     * @return a reference to the current value of this attribute instance
     */
    public T getValue() {
        return value;
    }
    
    /**
     * Replaces the value of an attribute instance by copying a reference to
     * the parameter {@code value}.
     * 
     * @param value reference to the new value of this attribute
     * @throws NullPointerException if {@code value} is {@code null}.
     */
    public void setValue(T value) {
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    /**
     * Returns a string representation of the typed attribute.
     * 
     * The {@code toString} method for class {@code TypedAttribute}
     * returns a string consisting of the attribute's type name, the opening
     * brace character `<tt>{</tt>', the string representation of the current
     * attribute's value, and the closing brace character `<tt>}</tt>'.
     * In other words, this method returns a string equal to the value of:
     * <blockquote>
     * <pre>
     * typeName() + '{' + value + '}'
     * </pre></blockquote>
     * @return a string representation of the typed attribute
     */
    @Override
    public String toString() {
        super.toString();
        return typeName() + '{' + value + '}';
    }

    /**
     * Returns a hash code for this typed attribute. The hash code for a 
     * {@code TypedAttribute<T>} object is computed as the aggregate of the 
     * hash values of {@link #typeName() } and {@link #value}.
     * 
     * @return a hash code for this typed attribute
     */
    @Override
    public int hashCode() {
        final String name = typeName();
        int hash = 3;
        hash = 83 * hash + (this.value != null ? this.value.hashCode() : 0);
        hash = 83 * hash + (name != null ? name.hashCode() : 0);
        return hash;
    }

    /**
     * Compares this typed attribute to the specified object.  The result is
     * {@code true} if and only if the argument is not {@code null}, is a
     * {@code TypedAttribute} of the same class as this object, their
     * {@link #typeName() } method returns the same string and their values
     * are equal.
     *
     * @param obj The object to compare this {@code TypedAttribute} against
     * @return {@code true} if the given object represents a
     *         {@code TypedAttribute} equivalent to this attribute,
     *         {@code false} otherwise
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TypedAttribute<T> other = (TypedAttribute<T>) obj;
        if (this.value != other.value && 
           (this.value == null || !this.value.equals(other.value))) {
            return false;
        }
        final String name = typeName();
        final String otherName = other.typeName();
        if ((name == null) ? (otherName != null) : !name.equals(otherName)) {
            return false;
        }
        return true;
    }
    
    /**
     * Clones the value, required by {@link #clone() }. Most of the times
     * instances should create a deep copy of their value.
     * 
     * @return a clone of the value.
     */
    protected abstract T cloneValue();

    @Override
    @SuppressWarnings("unchecked")
    public final TypedAttribute clone() {
        try {
            TypedAttribute<T> attr = (TypedAttribute<T>) super.clone();
            attr.value = this.cloneValue();
            return attr;
        } catch (CloneNotSupportedException ex) {
            throw new UnsupportedOperationException("Clone failed", ex);
        }
    }
    
}
