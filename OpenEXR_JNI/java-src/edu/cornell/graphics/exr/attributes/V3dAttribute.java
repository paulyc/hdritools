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
import edu.cornell.graphics.exr.ilmbaseto.Vector3;
import edu.cornell.graphics.exr.io.XdrInput;
import edu.cornell.graphics.exr.io.XdrOutput;

/**
 * A {@code TypedAttribute} subclass holding a {@code Vector3<Double>} value.
 * 
 * @since OpenEXR-JNI 2.1
 */
public final class V3dAttribute extends TypedAttribute<Vector3<Double>> {
    
    public V3dAttribute() {
        // empty
    }
    
    public V3dAttribute(Vector3<Double> value) {
        super(value);
    }

    @Override
    public String typeName() {
        return "v3d";
    }

    @Override
    protected void readValueFrom(XdrInput input, int version)
            throws EXRIOException {
        Vector3<Double> v = new Vector3<>();
        v.x = input.readDouble();
        v.y = input.readDouble();
        v.z = input.readDouble();
        setValue(v);
    }
    
    @Override
    protected void writeValueTo(XdrOutput output) throws EXRIOException {
        final Vector3<Double> v = getValue();
        output.writeDouble(v.x);
        output.writeDouble(v.y);
        output.writeDouble(v.z);
    }

    @Override
    protected Vector3<Double> cloneValue() {
        return new Vector3<>(value);
    }
    
}
