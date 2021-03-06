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

#if defined(__INTEL_COMPILER)
# include <mathimf.h>
#else
# include <cmath>
#endif

#include "TestUtil.h"
#include "Timer.h"
#include "dSFMT/RandomMT.h"

#include <Amaths.h>

namespace ssemath {
#include <sse_mathfun.h>
}

#include <gtest/gtest.h>

#include <iostream>
#ifdef _WIN32
# define NOMINMAX
# define WIN32_LEAN_AND_MEAN
# include <Windows.h>
#endif



namespace
{

union DataVec8
{
#if PCG_USE_AVX
    __m256   ymm;
#endif
    __m128   xmm[2];
    float    f32[8];
    uint32_t ui32[8];
    int32_t  i32[8];
};

} // namespace


using pcg::VarianceFunctor;
using std::cout;
using std::endl;

TEST(AMaths, Pow)
{
    RandomMT rnd(0x1e92ee2d);
    
    DataVec8 x;
    DataVec8 y;

    DataVec8 rAM;
    DataVec8 rCephes;
#if PCG_USE_AVX
    DataVec8 rAM_AVX;
    DataVec8 rCephes_AVX;
#endif
    float ref[8];
    const int N = 1000000;

    VarianceFunctor varAM_Rel;
    VarianceFunctor varAM_Abs;
    VarianceFunctor varCephes_Rel;
    VarianceFunctor varCephes_Abs;

    for (int i = 0; i < N; ++i) {

        // Initialize with values between -26 and 26 (as per the AMath docs)
        for (int k = 0; k != 8; ++k) {
            double tmp;

            // set x
            do {
                tmp = rnd.nextDouble();
            } while (tmp <= 1e-30);
            tmp *= 26.0;
            x.f32[k] = static_cast<float>(tmp);
            
            // set y (the exponent)
            const double log2X = 1.4426950408889f * log(x.f32[k]);
            do {
                tmp = rnd.nextDouble();
                tmp = tmp * 52.0 - 26.0;
                //tmp = tmp * 0.8 + 1.8;
            } while (std::abs(tmp) < 1e-30 || tmp * log2X > 127.499996185f);
            y.f32[k] = static_cast<float>(tmp);
        }

        // Calculate

        rAM.xmm[0] = am::pow_eps(x.xmm[0], y.xmm[0]);
        rAM.xmm[1] = am::pow_eps(x.xmm[1], y.xmm[1]);

        rCephes.xmm[0] = ssemath::pow_ps(x.xmm[0], y.xmm[0]);
        rCephes.xmm[1] = ssemath::pow_ps(x.xmm[1], y.xmm[1]);

#if PCG_USE_AVX
        rAM_AVX.ymm = am::pow_avx(x.ymm, y.ymm);
        rCephes_AVX.ymm = ssemath::pow_avx(x.ymm, y.ymm);
#endif

        for (int k = 0; k != 8; ++k) {
            ref[k] = powf(x.f32[k], y.f32[k]);
        }

        // Compare
        for (int k = 0; k != 8; ++k) {
#if PCG_USE_AVX
            // SSE and AVX have to generate the same values.
            ASSERT_EQ(rAM.f32[k], rAM_AVX.f32[k]);
# if !PCG_USE_AVX2
            // Values might not be exactly the same due to FMA in AVX2
            ASSERT_EQ(rCephes.f32[k], rCephes_AVX.f32[k]);
# else
            (void)rCephes_AVX;  // Silence gcc warning: unused-but-set-variable
# endif
#endif
            // Error between amath and stdlib
#if !PCG_USE_AVX
            double absErrorAM = std::abs(static_cast<double>(rAM.f32[k])-ref[k]);
#else
            double absErrorAM = std::abs(static_cast<double>(rAM_AVX.f32[k])-ref[k]);
#endif
            double relErrorAM = ref[k] != 0.0f ? std::abs(absErrorAM/ref[k])
                : (rAM.f32[k] == 0.0f ? 0.0 : absErrorAM);
            EXPECT_TRUE(absErrorAM < 1e-4 || relErrorAM < 5e-3) << 
                "Absolute error AM: " << absErrorAM <<
                "Relative error AM: " << relErrorAM <<
                " (" << x.f32[k] << " ** " << y.f32[k] << ")";
            varAM_Abs.update(absErrorAM);
            varAM_Rel.update(relErrorAM);

            // Error between cephes version and stdlib
            double absError = std::abs(static_cast<double>(ref[k]) - rCephes.f32[k]);
            double relError = ref[k] != 0.0f ? std::abs(absError/ref[k]) :
                (rCephes.f32[k] == 0.0f ? 0.0 : absError);
            ASSERT_TRUE(absError < 1e-8 || relError < 8e-6) << 
                "Absolute error: " << absError <<
                "Relative error: " << relError <<
                " (" << x.f32[k] << " ** " << y.f32[k] << ")";
            varCephes_Abs.update(absError);
            varCephes_Rel.update(relError);
        }
    }

    printf("AM Absolute error   | mean: %12g stddev: %12g max: %12g\n",
        varAM_Abs.mean(), varAM_Abs.stddev(), varAM_Abs.max());
    printf("AM Relative error   | mean: %12g stddev: %12g max: %12g\n",
        varAM_Rel.mean(), varAM_Rel.stddev(), varAM_Rel.max());
    printf("Cephes Absolute err | mean: %12g stddev: %12g max: %12g\n",
        varCephes_Abs.mean(), varCephes_Abs.stddev(), varCephes_Abs.max());
    printf("Cephes Relative err | mean: %12g stddev: %12g max: %12g\n",
        varCephes_Rel.mean(), varCephes_Rel.stddev(), varCephes_Rel.max());
}



TEST(AMaths, Pow_Benchmark)
{
#ifdef _WIN32
    SetThreadAffinityMask(GetCurrentThread(), 1);
#endif
    RandomMT rnd(0x7a36ea95);

    const int N = 4096*8;
    const int N_SSE = N / 4;

    float* valuesX = pcg::alloc_align<float>(32, N);
    float* valuesY = pcg::alloc_align<float>(32, N);
    float* result  = pcg::alloc_align<float>(32, N);

    // Initialize with values between -26 and 26 (as per the AMath docs)
    for (int k = 0; k != N; ++k) {
        double tmp;

        // set x
        do {
            tmp = rnd.nextDouble();
        } while (tmp <= 1e-30);
        tmp *= 26.0;
        valuesX[k] = static_cast<float>(tmp);
            
        // set y (the exponent)
        const double log2X = 1.4426950408889f * log(valuesX[k]);
        do {
            tmp = rnd.nextDouble();
            tmp = tmp * 52.0 - 26.0;
        } while (std::abs(tmp) < 1e-30 || tmp * log2X > 127.499996185f);
        valuesY[k] = static_cast<float>(tmp);
    }

    const __m128* sseX = reinterpret_cast<__m128*>(valuesX);
    const __m128* sseY = reinterpret_cast<__m128*>(valuesY);
    __m128* sseResult  = reinterpret_cast<__m128*>(result);

    Timer tAM_SSE;
    // Warmup
    for (int i = 0; i != 128; ++i) sseResult[i] = am::pow_eps(sseX[i], sseY[i]);
    tAM_SSE.start();
    for (int i = 0; i != N_SSE; ++i) {
        sseResult[i] = am::pow_eps(sseX[i], sseY[i]);
    }
    tAM_SSE.stop();
    cout << "  am::pow (SSE):    " << tAM_SSE.milliTime()*1e3 << " us" << endl;
    for (int i = 0; i != N; ++i) {
        ASSERT_GE(result[i], 0.0f);
    }

#if PCG_USE_AVX
    const int N_AVX = N / 8;
    const __m256* avxX = reinterpret_cast<__m256*>(valuesX);
    const __m256* avxY = reinterpret_cast<__m256*>(valuesY);
    __m256* avxResult  = reinterpret_cast<__m256*>(result);

    Timer tAM_AVX;
    // Warmup
    for (int i = 0; i != 64; ++i) avxResult[i] = am::pow_avx(avxX[i], avxY[i]);
    tAM_AVX.start();
    for (int i = 0; i != N_AVX; ++i) {
        avxResult[i] = am::pow_avx(avxX[i], avxY[i]);
    }
    tAM_AVX.stop();
    cout << "  am::pow (AVX):    " << tAM_AVX.milliTime()*1e3 << " us" << endl;
    cout << "  ## AVX/SSE: ##    "
         << tAM_AVX.milliTime()/tAM_SSE.milliTime()*100.0 << '%' << endl;
    for (int i = 0; i != N; ++i) {
        ASSERT_GE(result[i], 0.0f);
    }
#endif

    Timer tCephes_SSE;
    // Warmup
    for (int i = 0; i != 128; ++i) sseResult[i] = ssemath::pow_ps(sseX[i], sseY[i]);
    tCephes_SSE.start();
    for (int i = 0; i != N_SSE; ++i) {
        sseResult[i] = ssemath::pow_ps(sseX[i], sseY[i]);
    }
    tCephes_SSE.stop();
    cout << "  cephes pow (SSE): " << tCephes_SSE.milliTime()*1e3 << " us" << endl;
    for (int i = 0; i != N; ++i) {
        ASSERT_GE(result[i], 0.0f);
    }

#if PCG_USE_AVX
    Timer tCephes_AVX;
    // Warmup
    for (int i = 0; i != 64; ++i) avxResult[i] = ssemath::pow_avx(avxX[i], avxY[i]);
    tCephes_AVX.start();
    for (int i = 0; i != N_AVX; ++i) {
        avxResult[i] = ssemath::pow_avx(avxX[i], avxY[i]);
    }
    tCephes_AVX.stop();
    cout << "  cephes pow (AVX): " << tCephes_AVX.milliTime()*1e3 << " us" << endl;
    cout << "  ## AVX/SSE: ##    "
         << tCephes_AVX.milliTime()/tCephes_SSE.milliTime()*100.0 << '%' << endl;
    for (int i = 0; i != N; ++i) {
        ASSERT_GE(result[i], 0.0f);
    }
#endif

    Timer tRef;
    // Warmup
    for (int i = 0; i != 512; ++i) result[i] = powf(valuesX[i], valuesY[i]);
    tRef.start();
    for (int i = 0; i != N; ++i) {
        result[i] = powf(valuesX[i], valuesY[i]);
    }
    tRef.stop();
    cout << "  reference pow:    " << tRef.milliTime()*1e3 << " us" << endl;
    for (int i = 0; i != N; ++i) {
        ASSERT_GE(result[i], 0.0f);
    }

    pcg::free_align(valuesX);
    pcg::free_align(valuesY);
    pcg::free_align(result);
}



TEST(AMaths, Log)
{
    RandomMT rnd(0x1239fae3);
    
    DataVec8 x;
    DataVec8 rAM;
    DataVec8 rCephes;

#if PCG_USE_AVX
    DataVec8 rAM_AVX;
    DataVec8 rCephes_AVX;
#endif
    float ref[8];
    
    VarianceFunctor varAM_Rel;
    VarianceFunctor varAM_Abs;

    const int N = 1000000;
    for (int i = 0; i < N; ++i) {
        // Initialize with random values
        for (int k = 0; k != 8; ++k) {
            uint32_t& bits = x.ui32[k];
            do {
                bits = rnd.nextInt();
            } while (bits < 0x00800000 || bits >= 0x7f800000);
        }

        // Calculate
        rAM.xmm[0] = am::log_eps(x.xmm[0]);
        rAM.xmm[1] = am::log_eps(x.xmm[1]);

        rCephes.xmm[0] = ssemath::log_ps(x.xmm[0]);
        rCephes.xmm[1] = ssemath::log_ps(x.xmm[1]);

#if PCG_USE_AVX
        rAM_AVX.ymm = am::log_avx(x.ymm);
        rCephes_AVX.ymm = ssemath::log_avx(x.ymm);
#endif

        for (int k = 0; k != 8; ++k) {
            ref[k] = logf(x.f32[k]);
        }

        // Compare
        for (int k = 0; k != 8; ++k) {
#if PCG_USE_AVX
            ASSERT_FLOAT_EQ(rAM.f32[k], rAM_AVX.f32[k]);
            ASSERT_FLOAT_EQ(rCephes.f32[k], rCephes_AVX.f32[k]);
#endif
            ASSERT_NEAR(rAM.f32[k], ref[k], 5e-4f);
            ASSERT_FLOAT_EQ(ref[k], rCephes.f32[k]);

            // Error between amath and stdlib
            double absErrorAM = std::abs(static_cast<double>(rAM.f32[k])-ref[k]);
            double relErrorAM = ref[k] != 0.0f ? std::abs(absErrorAM/ref[k])
                : (rAM.f32[k] == 0.0f ? 0.0 : absErrorAM);
            varAM_Abs.update(absErrorAM);
            varAM_Rel.update(relErrorAM);
        }
    }

    printf("AM Absolute error   | mean: %12g stddev: %12g max: %12g\n",
        varAM_Abs.mean(), varAM_Abs.stddev(), varAM_Abs.max());
    printf("AM Relative error   | mean: %12g stddev: %12g max: %12g\n",
        varAM_Rel.mean(), varAM_Rel.stddev(), varAM_Rel.max());
}



TEST(AMaths, Log_Benchmark)
{
#ifdef _WIN32
    SetThreadAffinityMask(GetCurrentThread(), 1);
#endif
    RandomMT rnd(0xf3fbd7da);

    const int N = 4096*8;
    const int N_SSE = N / 4;

    float* valuesX = pcg::alloc_align<float>(32, N);
    float* result  = pcg::alloc_align<float>(32, N);

    // Initialize with random values
    for (int k = 0; k != N; ++k) {
        union { uint32_t bits; float f; };
        do {
            bits = rnd.nextInt();
        } while (bits < 0x00800000 || bits >= 0x7f800000);
        valuesX[k] = f;
    }

    const __m128* sseX = reinterpret_cast<__m128*>(valuesX);
    __m128* sseResult  = reinterpret_cast<__m128*>(result);

    Timer tAM_SSE;
    // Warmup
    for (int i = 0; i != 128; ++i) sseResult[i] = am::log_eps(sseX[i]);
    tAM_SSE.start();
    for (int i = 0; i != N_SSE; ++i) {
        sseResult[i] = am::log_eps(sseX[i]);
    }
    tAM_SSE.stop();
    cout << "  am::log (SSE):    " << tAM_SSE.milliTime()*1e3 << " us" << endl;
    for (int i = 0; i != N; ++i) {
        ASSERT_GT(result[i], -87.3365);
        ASSERT_LT(result[i],  88.7229);
    }

#if PCG_USE_AVX
    const int N_AVX = N / 8;
    const __m256* avxX = reinterpret_cast<__m256*>(valuesX);
    __m256* avxResult  = reinterpret_cast<__m256*>(result);

    Timer tAM_AVX;
    // Warmup
    for (int i = 0; i != 64; ++i) avxResult[i] = am::log_avx(avxX[i]);
    tAM_AVX.start();
    for (int i = 0; i != N_AVX; ++i) {
        avxResult[i] = am::log_avx(avxX[i]);
    }
    tAM_AVX.stop();
    cout << "  am::log (AVX):    " << tAM_AVX.milliTime()*1e3 << " us" << endl;
    cout << "  ## AVX/SSE: ##    "
         << tAM_AVX.milliTime()/tAM_SSE.milliTime()*100.0 << '%' << endl;
    for (int i = 0; i != N; ++i) {
        ASSERT_GT(result[i], -87.3365);
        ASSERT_LT(result[i],  88.7229);
    }
#endif

    Timer tCephes_SSE;
    // Warmup
    for (int i = 0; i != 128; ++i) sseResult[i] = ssemath::log_ps(sseX[i]);
    tCephes_SSE.start();
    for (int i = 0; i != N_SSE; ++i) {
        sseResult[i] = ssemath::log_ps(sseX[i]);
    }
    tCephes_SSE.stop();
    cout << "  cephes log (SSE): " << tCephes_SSE.milliTime()*1e3 << " us" << endl;
    for (int i = 0; i != N; ++i) {
        ASSERT_GT(result[i], -87.3365);
        ASSERT_LT(result[i],  88.7229);
    }

#if PCG_USE_AVX
    Timer tCephes_AVX;
    // Warmup
    for (int i = 0; i != 64; ++i) avxResult[i] = ssemath::log_avx(avxX[i]);
    tCephes_AVX.start();
    for (int i = 0; i != N_AVX; ++i) {
        avxResult[i] = ssemath::log_avx(avxX[i]);
    }
    tCephes_AVX.stop();
    cout << "  cephes log (AVX): " << tCephes_AVX.milliTime()*1e3 << " us" << endl;
    cout << "  ## AVX/SSE: ##    "
         << tCephes_AVX.milliTime()/tCephes_SSE.milliTime()*100.0 << '%' << endl;
    for (int i = 0; i != N; ++i) {
        ASSERT_GT(result[i], -87.3365);
        ASSERT_LT(result[i],  88.7229);
    }
#endif

    Timer tRef;
    // Warmup
    for (int i = 0; i != 512; ++i) result[i] = logf(valuesX[i]);
    tRef.start();
    for (int i = 0; i != N; ++i) {
        result[i] = logf(valuesX[i]);
    }
    tRef.stop();
    cout << "  reference log:    " << tRef.milliTime()*1e3 << " us" << endl;
    for (int i = 0; i != N; ++i) {
        ASSERT_GT(result[i], -87.3365);
        ASSERT_LT(result[i],  88.7229);
    }

    pcg::free_align(valuesX);
    pcg::free_align(result);
}



namespace
{

inline double fastExp(double y) {
    // Super cheap approximation of exp(y) from:
    // http://nic.schraudolph.org/pubs/Schraudolph99.pdf
    // It has *very* poor accuracy (about 1 decimal digit!)
    //
    // i = a*y + (b-c)
    //For doubles:
    //  a = 2^20 / ln(2)
    //  b = 1023 * 2^23
    //  c = "magic constant"

    // ln(2)   ~= 0.69314718055994530941723212145818
    // 1/ln(2) ~= 1.4426950408889634073599246810019

    const double EXP_A = (1 << 20) * 1.4426950408889634073599246810019;
    const double EXP_B = 1023.0 * (1 << 20);
    const double EXP_C = 60801.48;

    union {
        struct {
            int32_t lo;
            int32_t hi;
        } x;
        double fp64;
    };
    x.lo = 0;
    x.hi = static_cast<int32_t>(EXP_A * (y) + (EXP_B - EXP_C));
    return fp64;
}

} // namespace

TEST(AMaths, FastExpGamma)
{
    // Test the accuracy of fastExp as a way to calculate gamma 2.2:
    // pow(x, 1/2.2) = exp(1/2.2 * log(x))

    VarianceFunctor varAbs, varRel;
    RandomMT rnd(0x819a151e);
    const int N = 100000;

    for (int i = 0; i < N; ++i) {
        double x    = rnd.nextDouble();
        double logX = log(x);
        double ref    = exp((1.0/2.2) * logX);
        double actual = fastExp((1.0/2.2) * logX);

        double absError = std::abs(ref - actual);
        double relError = ref != 0.0f ? std::abs(absError/ref)
                                      : (actual == 0.0f ? 0.0 : absError);
        
        ASSERT_NEAR(actual, ref, 0.04*ref) << "x=" << x << " log(x)=" << logX;
        varAbs.update(absError);
        varRel.update(relError);
    }

    printf("FastExp Absolute error   | mean: %-10g stddev: %-10g max: %-10g\n",
        varAbs.mean(), varAbs.stddev(), varAbs.max());
    printf("FastExp Relative error   | mean: %-10g stddev: %-10g max: %-10g\n",
        varRel.mean(), varRel.stddev(), varRel.max());
}
