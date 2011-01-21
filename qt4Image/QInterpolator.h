#if !defined(PCG_QINTERPOLATOR)
#define PCG_QINTERPOLATOR

#include <QAbstractSlider>
#include <QLineEdit>
#include <QPointer>
#include "QFixupDoubleValidator.h"

// Class responsible to map a range of values between a line-edit and a slider
// with validation while keeping both controls synchronized. It also emmits a
// signal when the value is changed.
class QInterpolator : public QObject
{
    Q_OBJECT

public:
    QInterpolator(double minimum, double maximum,
        QAbstractSlider *slider, QLineEdit *edit, QObject *parent = 0);

    void setRange(double minimum, double maximum);

    void setValue(double value);
    inline double value() const {
        return m_value;
    }


protected:

    // To be implemented by the subclasses:

    // Function called to update the internal state. The default
    // implementation doesn't do anything!
    virtual void updateState(double minimum, double maximum,
        int sliderMinimum, int sliderMaximum)
    {
    }

    // Given a certain value (within the valid range), calculate the
    // appropriate value it maps to into the slider
    virtual int toSliderValue(double value) = 0;

    // Given a certain slider value (within the valid range), calculate
    // the value it maps to
    virtual double toValue(int sliderValue) = 0;

    inline double top() {
        return m_validator.top();
    }

    inline double bottom() {
        return m_validator.bottom();
    }

    inline int sliderMinimum() {
        return !m_slider.isNull() ? m_slider->minimum() : -1;
    }

    inline int sliderMaximum() {
        return !m_slider.isNull() ? m_slider->maximum() : -1;
    }


signals:
    void valueChanged(double value);
    

private slots:
    void sliderRangeChanged(int minimum, int maximum);
    void sliderChanged(int sliderValue);
    void textEdited();

private:
    QPointer<QAbstractSlider> m_slider;
    QPointer<QLineEdit> m_edit;
    QFixupDoubleValidator m_validator;
    double m_value;
};



// Concrete implementation which implements simple linear interpolation
class QLinearInterpolator : public QInterpolator
{
    Q_OBJECT

public:
    QLinearInterpolator(double minimum, double maximum,
        QAbstractSlider *slider, QLineEdit *edit, QObject *parent = 0);

protected:
    virtual void updateState(double minimum, double maximum,
        int sliderMinimum, int sliderMaximum);
    virtual int toSliderValue(double value);
    virtual double toValue(int sliderValue);

private:
    double m_slope;
    double m_slopeInv;
    double m_intercept;
};



// Concrete implementation using a power law
class QPowerInterpolator : public QInterpolator
{
    Q_OBJECT

public:
    QPowerInterpolator(double exponent, double minimum, double maximum,
        QAbstractSlider *slider, QLineEdit *edit, QObject *parent = 0);

    inline double exponent() const {
        return m_exponent;
    }

    void setExponent(double value);

protected:
    virtual void updateState(double minimum, double maximum,
        int sliderMinimum, int sliderMaximum);
    virtual int toSliderValue(double value);
    virtual double toValue(int sliderValue);

private:
    double m_exponent;
    double m_exponentInv;

    double m_valueRange;
    double m_valueRangeInv;
    double m_valueMin;
    double m_sliderRange;
    double m_sliderRangeInv;
    double m_sliderMin;
};



#endif // PCG_QINTERPOLATOR
