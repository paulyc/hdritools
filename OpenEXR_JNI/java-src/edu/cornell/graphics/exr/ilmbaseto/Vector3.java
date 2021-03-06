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
 * Transfer object corresponding to the class {@code Imath::Vector3<class T>} of
 * the IlmBase C++ library.
 * 
 * @since OpenEXR-JNI 2.1
 */
public class Vector3<T extends Number> {
    public T x;
    public T y;
    public T z;
    
    /**
     * Default constructor. Creates an uninitialized vector.
     */
    public Vector3() {
        // empty
    }
    
    /**
     * Copy constructor. Initializes each element from the corresponding ones
     * in {@code other}.
     * @param other a vector
     */
    public Vector3(Vector3<T> other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }
    
    /**
     * Initializes each element of a newly created {@code Vector3} with specific
     * values.
     * 
     * @param x initial value for {@code this.x}
     * @param y initial value for {@code this.y}
     * @param z initial value for {@code this.z}
     */
    public Vector3(T x, T y, T z) {
        this.x = x;
        this.y = y;
        this.z = z;
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
        final Vector3<T> other = (Vector3<T>) obj;
        if (this.x != other.x && (this.x == null || !this.x.equals(other.x))) {
            return false;
        }
        if (this.y != other.y && (this.y == null || !this.y.equals(other.y))) {
            return false;
        }
        if (this.z != other.z && (this.z == null || !this.z.equals(other.z))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + (this.x != null ? this.x.hashCode() : 0);
        hash = 53 * hash + (this.y != null ? this.y.hashCode() : 0);
        hash = 53 * hash + (this.z != null ? this.z.hashCode() : 0);
        return hash;
    }
    
    @Override
    public String toString() {
        return "(" + x + " " + y + " " + z + ")";
    }
}
