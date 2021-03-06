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

package edu.cornell.graphics.exr.ilmbaseto;

/**
 * Transfer object corresponding to the class {@code Imath::Vector2<class T>} of
 * the IlmBase C++ library.
 * 
 * @since OpenEXR-JNI 2.1
 */
public class Vector2<T extends Number> {
    public T x;
    public T y;
    
    /**
     * Default constructor. Creates an uninitialized vector.
     */
    public Vector2() {
        // empty
    }
    
    /**
     * Copy constructor. Initializes each element from the corresponding ones
     * in {@code other}.
     * @param other a vector
     */
    public Vector2(Vector2<T> other) {
        this.x = other.x;
        this.y = other.y;
    }
    
    /**
     * Initializes each element of a newly created {@code Vector2} with specific
     * values.
     * 
     * @param x initial value for {@code this.x}
     * @param y initial value for {@code this.y}
     */
    public Vector2(T x, T y) {
        this.x = x;
        this.y = y;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Vector2<T> other = (Vector2<T>) obj;
        if (this.x != other.x && (this.x == null || !this.x.equals(other.x))) {
            return false;
        }
        if (this.y != other.y && (this.y == null || !this.y.equals(other.y))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + (this.x != null ? this.x.hashCode() : 0);
        hash = 59 * hash + (this.y != null ? this.y.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "(" + x + " " + y + ")";
    }
}
