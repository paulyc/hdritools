/*============================================================================
  HDRITools - High Dynamic Range Image Tools
  Copyright 2008-2011 Program of Computer Graphics, Cornell University

  Distributed under the OSI-approved MIT License (the "License");
  see accompanying file LICENSE for details.

  This software is distributed WITHOUT ANY WARRANTY; without even the
  implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  See the License for more information.
 ----------------------------------------------------------------------------- 
 Primary author:
     Edgar Velazquez-Armendariz <cs#cornell#edu - eva5>
============================================================================*/

// Construct a bidimensional array with the different floating point versions
// of the numbers [0,255] to avoid a cast everytime we convert rgbe pixels.
// The other dimension of the array has the LUT for the exponents:
// (float)(ldexp(1.0,exp-(int)(128+8)))
// for all possible values of exp [0,255], but at 0 it always returns 0

#include <iostream>
#include <iomanip>
#include <cmath>

using namespace std;

int main() {

	cout.precision (9);
	cout.setf (ios_base::hex, ios_base::basefield);

	cout << "//\n"
		"// This is an automatically generated file.\n"
		"// Do not edit.\n"
		"//\n\n";

	cout << 
		"{\n  {\n"
		"    // Bidimensional array with the different floating point versions\n" 
		"    // of the numbers [0,255] to avoid a cast everytime we convert rgbe "
		"pixels.\n    ";

	const int iMax = (1 << 8);

	for (int i = 0; i < iMax; i++)
	{
		union { float f; unsigned int val; };
		f = static_cast<float>(i);
		cout << "0x" << setfill ('0') << setw (8) << val << ", ";

		if (i % 4 == 3)
		{
			cout << "\n";

			if (i < iMax - 1)
				cout << "    ";
		}
	}

	// Exponent LUT
	cout << 
		"  },\n  {\n"
		"    // LUT for the exponents: (float)(ldexp(1.0,exp-(int)(128+8)))\n"
		"    // for all possible values of exp [0,255], but at 0 it always "
		"returns 0.\n    ";

	for (int i = 0; i < iMax; i++)
	{
		union { float f; unsigned int val; };
		f = i > 0 ? ldexp(1.0f,i-(128+8)) : 0.0f;
		cout << "0x" << setfill ('0') << setw (8) << val << ", ";

		if (i % 4 == 3)
		{
			cout << "\n";

			if (i < iMax - 1)
				cout << "    ";
		}
	}

	cout << "  }\n};\n";
	return 0;
}
