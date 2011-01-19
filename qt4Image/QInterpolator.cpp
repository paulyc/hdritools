#include "QInterpolator.h"
#include <QDebug>

QInterpolator::QInterpolator(double minimum, double maximum,
                             QAbstractSlider *slider, QLineEdit *edit,
                             QObject *parent):
QObject(parent),
m_slider(slider), m_edit(edit), m_validator(minimum, maximum, 16, this),
m_value(0.0)
{
    Q_ASSERT(!m_slider.isNull());
    Q_ASSERT(!m_edit.isNull());
    Q_ASSERT(minimum < maximum);
    m_edit->setValidator(&m_validator);
    setRange(minimum, maximum);

    // Connect the signals
    connect ( m_slider, SIGNAL(rangeChanged(int,int)),
        this, SLOT(sliderRangeChanged(int,int)) );
    connect ( m_slider, SIGNAL(valueChanged(int)), 
        this, SLOT(sliderChanged(int)) );
    connect ( m_edit, SIGNAL(editingFinished(void)),
        this, SLOT(textEdited(void)) );
}


const void QInterpolator::setRange(double minimum, double maximum)
{
    Q_ASSERT(minimum < maximum);
    m_validator.setRange(minimum, maximum, m_validator.decimals());
    updateState(minimum, maximum, m_slider->minimum(), m_slider->maximum());
}


const void QInterpolator::setValue(double value)
{
    value = qBound(bottom(), value, top());
    if (!qFuzzyCompare(value, m_value)) {
        m_value = value;

        bool textOk;
        const double textValue = m_edit->text().toDouble(&textOk);
        if (!textOk || !qFuzzyCompare(value, textValue)) {
            QString text = QString::number(value);
            int txtPos;
            QValidator::State state = m_validator.validate(text, txtPos);
            if (state != QValidator::Acceptable) {
                m_validator.fixup(text);
            }
            Q_ASSERT(m_validator.validate(text, txtPos) == QValidator::Acceptable);
            m_edit->setText(text);
        }

        const int pos = toSliderValue(value);
        if (pos != m_slider->value()) {
            Q_ASSERT(m_slider->minimum() <= pos && pos <= m_slider->maximum());
            m_slider->setValue(pos);
        }

        emit valueChanged(value);
    }
}


void QInterpolator::sliderRangeChanged(int minimum, int maximum)
{
    updateState(bottom(), top(), minimum, maximum);
}


void QInterpolator::sliderChanged(int sliderValue)
{
    // Don't update if the current value would map to the same slider position
    if (sliderValue != toSliderValue(m_value)) {
        const double value = toValue(sliderValue);
        Q_ASSERT(bottom() <= value && value <= top());
        setValue(value);
    }
}


void QInterpolator::textEdited()
{
    bool ok;
    const double value = m_edit->text().toDouble(&ok);
    Q_ASSERT(ok);
    setValue(value);
}



QLinearInterpolator::QLinearInterpolator(double minimum, double maximum,
                                         QAbstractSlider *slider,
                                         QLineEdit *edit, QObject *parent)
: QInterpolator(minimum, maximum, slider, edit, parent),
  m_slope(0.0), m_slopeInv(0.0), m_intercept(0.0)
{
    // C++ doesn't like virtual functions inside constructors
    updateState(minimum, maximum, slider->minimum(), slider->maximum());
}


void QLinearInterpolator::updateState(double minimum, double maximum,
                                      int sliderMinimum, int sliderMaximum)
{
    m_slope = (maximum - minimum) / 
        static_cast<double>(sliderMaximum - sliderMinimum);
    m_slopeInv = 1.0/m_slope;
    m_intercept = minimum - m_slope*static_cast<double>(sliderMinimum);
}


int QLinearInterpolator::toSliderValue(double value)
{
    Q_ASSERT(m_slopeInv != 0.0);
    int pos = qRound((value - bottom())*m_slopeInv + 
        static_cast<double>(sliderMinimum()));
    return pos;
}


double QLinearInterpolator::toValue(int sliderValue)
{
    Q_ASSERT(m_slope != 0.0);
    double value = m_slope*static_cast<double>(sliderValue) + m_intercept;
    return value;
}