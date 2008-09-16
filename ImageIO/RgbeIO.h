// IO operation to create and load RGBE files

#if !defined(PCG_RGBEIO_H)
#define PCG_RGBEIO_H

#include "ImageIO.h"
#include "rgbe.h"
#include "Rgba32F.h"
#include "Rgb32F.h"
#include "Image.h"

namespace pcg {

	// To keep the code simple, we only allow to load the common formats.
	// This way we can instanciate the templates in the cpp and have slimmer headers
	class RgbeIO {

	public:

		// ### Load functions ###

		// Rgbe pixels
		static IMAGEIO_API void Load(Image<Rgbe,TopDown>  &img, istream &is);
		static IMAGEIO_API void Load(Image<Rgbe,BottomUp> &img, istream &is);
		static IMAGEIO_API void Load(Image<Rgbe,TopDown>  &img, const char *filename, bool closeStream = true);
		static IMAGEIO_API void Load(Image<Rgbe,BottomUp> &img, const char *filename, bool closeStream = true);

		// Rgba32F pixels
		static IMAGEIO_API void Load(Image<Rgba32F,TopDown>  &img, istream &is);
		static IMAGEIO_API void Load(Image<Rgba32F,BottomUp> &img, istream &is);
		static IMAGEIO_API void Load(Image<Rgba32F,TopDown>  &img, const char *filename, bool closeStream = true);
		static IMAGEIO_API void Load(Image<Rgba32F,BottomUp> &img, const char *filename, bool closeStream = true);

		// Rgb32F pixels
		static IMAGEIO_API void Load(Image<Rgb32F,TopDown>  &img, istream &is);
		static IMAGEIO_API void Load(Image<Rgb32F,BottomUp> &img, istream &is);
		static IMAGEIO_API void Load(Image<Rgb32F,TopDown>  &img, const char *filename, bool closeStream = true);
		static IMAGEIO_API void Load(Image<Rgb32F,BottomUp> &img, const char *filename, bool closeStream = true);


		// ### Save functions ###

		// Rgbe pixels
		static IMAGEIO_API void Save(Image<Rgbe,TopDown>  &img, ostream &os);
		static IMAGEIO_API void Save(Image<Rgbe,BottomUp> &img, ostream &os);
		static IMAGEIO_API void Save(Image<Rgbe,TopDown>  &img, const char *filename, bool closeStream = true);
		static IMAGEIO_API void Save(Image<Rgbe,BottomUp> &img, const char *filename, bool closeStream = true);

		// Rgba32F pixels
		static IMAGEIO_API void Save(Image<Rgba32F,TopDown>  &img, ostream &os);
		static IMAGEIO_API void Save(Image<Rgba32F,BottomUp> &img, ostream &os);
		static IMAGEIO_API void Save(Image<Rgba32F,TopDown>  &img, const char *filename, bool closeStream = true);
		static IMAGEIO_API void Save(Image<Rgba32F,BottomUp> &img, const char *filename, bool closeStream = true);

		// Rgb32F pixels
		static IMAGEIO_API void Save(Image<Rgb32F,TopDown>  &img, ostream &os);
		static IMAGEIO_API void Save(Image<Rgb32F,BottomUp> &img, ostream &os);
		static IMAGEIO_API void Save(Image<Rgb32F,TopDown>  &img, const char *filename, bool closeStream = true);
		static IMAGEIO_API void Save(Image<Rgb32F,BottomUp> &img, const char *filename, bool closeStream = true);

	};


}


#endif /* PCG_RGBEIO_H */
