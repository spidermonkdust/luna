package io.luna.util;

import java.math.BigInteger;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A model representing a rational number.
 *
 * @author lare96 <http://github.org/lare96>
 */
public final class Rational extends Number {

    /**
     * The numerator.
     */
    private final int numerator;

    /**
     * The denominator.
     */
    private final int denominator;

    /**
     * Create a {@link Rational}.
     *
     * @param numerator The numerator.
     * @param denominator The denominator.
     */
    public Rational(int numerator, int denominator) {
        checkArgument(denominator != 0, "denominator cannot be 0");

        if (denominator < 0) {
            numerator = numerator * -1;
            denominator = denominator * -1;
        }

        if (numerator != 0) {
            int gcd = gcd(Math.abs(numerator), denominator);

            numerator = numerator / gcd;
            denominator = denominator / gcd;
        }

        this.numerator = numerator;
        this.denominator = denominator;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (numerator == 0) {
            sb.append("0");
        } else if (denominator == 1) {
            sb.append(numerator);
        } else {
            sb.append(numerator).append("/").append(denominator);
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(numerator, denominator);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Rational) {
            Rational other = (Rational) obj;
            return numerator == other.numerator && denominator == other.denominator;
        }
        return false;
    }

    /**
     * Determines if this rational is greater than another.
     */
    public boolean greaterThan(Rational rational) {
        return doubleValue() > rational.doubleValue();
    }

    /**
     * Determines if this rational is less than another.
     */
    public boolean lessThan(Rational rational) {
        return doubleValue() < rational.doubleValue();
    }

    @Override
    public int intValue() {
        return numerator / denominator;
    }

    @Override
    public long longValue() {
        return (long) intValue();
    }

    @Override
    public float floatValue() {
        return ((float) numerator / (float) denominator);
    }

    @Override
    public double doubleValue() {
        return ((double) numerator / (double) denominator);
    }

    /**
     * Returns the reciprocal of this rational.
     */
    public Rational reciprocal() {
        return new Rational(denominator, numerator);
    }

    /**
     * Adds to this rational.
     */
    public Rational add(Rational rational) {
        int commonDenominator = denominator * rational.denominator;
        int numeratorOne = numerator * rational.denominator;
        int numeratorTwo = rational.numerator * denominator;
        int numeratorSum = numeratorOne + numeratorTwo;

        return new Rational(numeratorSum, commonDenominator);
    }

    /**
     * Subtracts from this rational.
     */
    public Rational subtract(Rational rational) {
        int commonDenominator = denominator * rational.denominator;
        int numeratorOne = numerator * rational.denominator;
        int numeratorTwo = rational.numerator * denominator;
        int numeratorDifference = numeratorOne - numeratorTwo;

        return new Rational(numeratorDifference, commonDenominator);
    }

    /**
     * Multiplies this rational.
     */
    public Rational multiply(Rational rational) {
        int n = numerator * rational.numerator;
        int d = denominator * rational.denominator;

        return new Rational(n, d);
    }

    /**
     * Divides this rational.
     */
    public Rational divide(Rational rational) {
        return multiply(rational.reciprocal());
    }

    /**
     * Determines the greatest common denominator.
     */
    private int gcd(int numeratorOne, int numeratorTwo) {
        BigInteger numOne = BigInteger.valueOf(numeratorOne);
        BigInteger numTwo = BigInteger.valueOf(numeratorTwo);
        return numOne.gcd(numTwo).intValue();
    }

    /**
     * @return The numerator.
     */
    public int getNumerator() {
        return numerator;
    }

    /**
     * @return The denominator.
     */
    public int getDenominator() {
        return denominator;
    }
}
