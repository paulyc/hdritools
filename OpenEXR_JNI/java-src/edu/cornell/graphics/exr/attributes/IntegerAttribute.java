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

/**
 * A {@code TypedAttribute} subclass holding an {@code Integer} value.
 * 
 * @since OpenEXR-JNI 2.1
 */
public final class IntegerAttribute extends TypedAttribute<Integer> {
    
    public IntegerAttribute() {
        // empty
    }
    
    public IntegerAttribute(int value) {
        super(value);
    }

    @Override
    public String typeName() {
        return "int";
    }

    @Override
    protected void readValueFrom(XdrInput input, int version)
            throws EXRIOException {
        int i = input.readInt();
        setValue(i);
    }
    
    @Override
    protected void writeValueTo(XdrOutput output) throws EXRIOException {
        int v = getValue();
        output.writeInt(v);
    }

    @Override
    protected Integer cloneValue() {
        // Integer objects are immutable
        return value;
    }
    
}
