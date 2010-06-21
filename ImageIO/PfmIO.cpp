#include "PfmIO.h"

#include <sstream>
#include <fstream>
#include <iomanip>
#include <ctype.h>
#include <iostream>

using namespace pcg;

inline PfmIO::ByteOrder PfmIO::getNativeOrder()
{
	const int x = 1;
	return (*(char *)&x == 1) ? PfmIO::LittleEndian : PfmIO::BigEndian;
}

PfmIO::Header::Header() : 
isColor(true), width(0), height(0), order(PfmIO::getNativeOrder())
{
}

PfmIO::Header::Header(const Image<Rgba32F, TopDown> &img) :
isColor(true), width(img.Width()), height(img.Height()), order(PfmIO::getNativeOrder())
{
}

PfmIO::Header::Header(const Image<Rgba32F, BottomUp> &img) :
isColor(true), width(img.Width()), height(img.Height()), order(PfmIO::getNativeOrder())
{
}

PfmIO::Header::Header(istream &is)
{
	{
		// Look for the magic number, consuming also the \n
		char a, b, junk;
		is >> a;
		is >> b;
		is.read(&junk, 1);

		if ((a != 'P') || ((b != 'F') && (b != 'f'))) {
			throw PfmIOException("Wrong magic number");
		}
		
		isColor = (b == 'F');	// 'F' = RGB,  'f' = monochrome

		std::string buffer;
		for (;;) {
			std::getline(is, buffer);
			if ( is.fail() ) {
				throw PfmIOException("Couldn't read the header");
			}
			else if (buffer[0] != '#') {
				// Buffer contains the width and the height
				std::istringstream buf(buffer);
				buf >> width;
				if ( buf.fail() ) {
					throw PfmIOException("Couldn't read the width");
				}
				buf >> height;
				if ( buf.fail() ) {
					throw PfmIOException("Couldn't read the width");
				}
				break;
			}
		}

		// Now read the line order and the trailing space
		float orderFloat;
		is >> orderFloat;
		if ( is.fail() ) {
			throw PfmIOException("Couldn't read the byte order");
		}
		is.read(&junk, 1);
		if ( is.fail() ) {
			throw PfmIOException("Couldn't read the separator "
				"between the header and the data");
		}
		order = orderFloat <= 0.0f ? PfmIO::LittleEndian : PfmIO::BigEndian;
	}
}


void PfmIO::Header::write(std::ostream &os)
{
	os << (isColor ? "PF" : "Pf") << '\n' << 
		   width << ' ' << height << '\n' << 
		   std::setiosflags(ios::fixed) << std::setprecision(6) <<
		  (float)(order == PfmIO::LittleEndian ? -1.0f : 1.0f) << '\n';
}


template <ScanLineMode S>
void PfmIO_Save_data(const Image<Rgba32F, S>  &img, ostream &os)
{
	// Allocate one full scanline
	const size_t scanline_len = img.Width() * 3 * sizeof(float);
	float *buffer = new float[scanline_len];

	for (int i = 0; i < img.Height(); ++i) {
		const Rgba32F *ptr = img.GetScanlinePointer(i, BottomUp);
		for (int j = 0; j < img.Width(); ++j) {
			const int idx = j*3;
			buffer[idx]   = ptr[j].r();
			buffer[idx+1] = ptr[j].g();
			buffer[idx+2] = ptr[j].b();
		}
		os.write((const char*)buffer, scanline_len);
		if (os.fail()) {
			delete [] buffer;
			throw PfmIOException("Couldn't write the scanline data");
		}
	}

	delete [] buffer;
}

void PfmIO::Save(const Image<Rgba32F, TopDown>  &img, ostream &os)
{
	Header hdr(img);
	hdr.write(os);
	PfmIO_Save_data(img, os);
}

void PfmIO::Save(const Image<Rgba32F, BottomUp>  &img, ostream &os)
{
	Header hdr(img);
	hdr.write(os);
	PfmIO_Save_data(img, os);
}


inline void swapByteOrder(unsigned int *ptr, int count)
{
	for (int i = 0; i < count; ++i) {
		ptr[i] = ((ptr[i] && 0xFF) << 24)    || ((ptr[i] && 0xFF00) << 8) || 
			     ((ptr[i] && 0xFF0000) >> 8) || ((ptr[i] && 0xFF000000) >> 24);
	}
}

// Load function just for the data, assumes the istream is right
// at the beginning of the pixels and the image has been allocated
template <ScanLineMode S>
void Pfm_Load_data(Image<Rgba32F, S> &img, istream &is, 
				   bool swapBytes, bool isColor)
{
	// TODO Properly read grayscale images
	if (!isColor) {
		throw PfmIOException("Grayscale data not yet supported");
	}

	// Allocate one full scanline
	const size_t scanline_len = img.Width() * 3 * sizeof(float);
	float *buffer = new float[scanline_len];

	for (int h = 0; h < img.Height(); ++h) {
		is.read((char*)buffer, scanline_len);
		if ( is.fail() ) {
			delete [] buffer;
			throw PfmIOException("Couldn't read all the scanline data ");
		}

		if (swapBytes) {
			swapByteOrder((unsigned int *)buffer, img.Width() * 3);
		}

		Rgba32F *ptr = img.GetScanlinePointer(h, BottomUp);
		for (int i = 0; i < img.Width(); ++i) {
			const int idx = i*3;
			ptr[i].set(buffer[idx], buffer[idx+1], buffer[idx+2]);
		}
	}

	delete [] buffer;
}


void PfmIO::Load(Image<Rgba32F, TopDown> &img, istream &is)
{
	// Read the header
	Header hdr(is);

	// Allocates the space
	img.Alloc(hdr.width, hdr.height);

	// Reads the pixels
	Pfm_Load_data(img, is, hdr.order != getNativeOrder(), hdr.isColor);
}

void PfmIO::Load(Image<Rgba32F, BottomUp> &img, istream &is)
{
	// Read the header
	Header hdr(is);

	// Allocates the space
	img.Alloc(hdr.width, hdr.height);

	// Reads the pixels
	Pfm_Load_data(img, is, hdr.order != getNativeOrder(), hdr.isColor);
}


template <ScanLineMode S>
void PfmIO_Save_helper(const Image<Rgba32F, S> &img, 
					   const char *filename, bool closeStream)
{
	ofstream pfmFile(filename, ios_base::binary);
	if (! pfmFile.bad() ) {
		PfmIO::Save(img, pfmFile);
		if (closeStream) {
			pfmFile.close();
		}
	}
	else {
		// Something terrible takes place here
		throw PfmIOException((std::string)"Couldn't save the file " + filename);
	}
}

template <ScanLineMode S>
void PfmIO_Load_helper(Image<Rgba32F, S> &img, 
					   const char *filename, bool closeStream) 
{
	ifstream pfmFile(filename, ios_base::binary);
	if (! pfmFile.bad() ) {
		PfmIO::Load(img, pfmFile);
		if (closeStream) {
			pfmFile.close();
		}
	}
	else {
		// Something terrible takes place here
		throw PfmIOException((std::string)"Couldn't open the file " + filename);
	}
}


// Instanciate the templates
void PfmIO::Save(const Image<Rgba32F, TopDown> &img, 
				 const char *filename, bool closeStream) 
{
	PfmIO_Save_helper(img, filename, closeStream);
}
void PfmIO::Save(const Image<Rgba32F, BottomUp> &img, 
				 const char *filename, bool closeStream) 
{
	PfmIO_Save_helper(img, filename, closeStream);
}


void PfmIO::Load(Image<Rgba32F, TopDown>  &img, 
				 const char *filename, bool closeStream)
{
	PfmIO_Load_helper(img, filename, closeStream);
}
void PfmIO::Load(Image<Rgba32F, BottomUp> &img, 
				 const char *filename, bool closeStream)
{
	PfmIO_Load_helper(img, filename, closeStream);
}