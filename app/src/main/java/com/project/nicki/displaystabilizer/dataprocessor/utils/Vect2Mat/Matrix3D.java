/*
Java classes to implement 3D transformation matrices

      by Ken Perlin @ NYU, 1998.
You have my permission to use freely, as long as you keep the attribution. - Ken Perlin

Note: this Matrix3D.html file also works as a legal Matrix3D.java file. If you save the source under that name, you can just run javac on it.

Why does this class exist?

I created this class to support general purpose 3D transformations. I use it in a number of the demos that run on my Web page.
What does the class do?

You can use it to create 3D points and homogeneous vectors, and also to create transformation matrices with these. There are methods to rotate, translate, and scale transformations, and to apply transformations to vectors. You can also get and set the elements of matrices and vectors.
The classes Vector3D and Matrix3D are extended from respective generic classes VectorN and MatrixN, which do most of the bookkeeping for arithmetic vectors of length N and square matrices of size N × N, respectively.

*/
package com.project.nicki.displaystabilizer.dataprocessor.utils.Vect2Mat;
// Homogeneous transformation matrices in three dimensions

public class Matrix3D extends MatrixN {

    public Matrix3D() { // create a new identity transformation
        super(4);
        identity();
    }

    public void rotateX(double theta) { // rotate transformation about the X axis

        Matrix3D tmp = new Matrix3D();
        double c = Math.cos(theta);
        double s = Math.sin(theta);

        tmp.set(1,1, c);
        tmp.set(1,2,-s);
        tmp.set(2,1, s);
        tmp.set(2,2, c);

        preMultiply(tmp);
    }
    public void rotateY(double theta) { // rotate transformation about the Y axis

        Matrix3D tmp = new Matrix3D();
        double c = Math.cos(theta);
        double s = Math.sin(theta);

        tmp.set(2,2, c);
        tmp.set(2,0,-s);
        tmp.set(0,2, s);
        tmp.set(0,0, c);

        preMultiply(tmp);
    }
    public void rotateZ(double theta) { // rotate transformation about the Z axis

        Matrix3D tmp = new Matrix3D();
        double c = Math.cos(theta);
        double s = Math.sin(theta);

        tmp.set(0,0, c);
        tmp.set(0,1,-s);
        tmp.set(1,0, s);
        tmp.set(1,1, c);

        preMultiply(tmp);
    }

    public void translate(double a, double b, double c) { // translate

        Matrix3D tmp = new Matrix3D();

        tmp.set(0,3, a);
        tmp.set(1,3, b);
        tmp.set(2,3, c);

        preMultiply(tmp);
    }
    public void translate(Vector3D v) { translate(v.get(0), v.get(1), v.get(2)); }

    public void scale(double s) { // scale uniformly

        Matrix3D tmp = new Matrix3D();

        tmp.set(0,0, s);
        tmp.set(1,1, s);
        tmp.set(2,2, s);

        preMultiply(tmp);
    }
    public void scale(double r, double s, double t) { // scale non-uniformly

        Matrix3D tmp = new Matrix3D();

        tmp.set(0,0, r);
        tmp.set(1,1, s);
        tmp.set(2,2, t);

        preMultiply(tmp);
    }
    public void scale(Vector3D v) { scale(v.get(0), v.get(1), v.get(2)); }
}

// Homogeneous vectors in three dimensions

// Geometric vectors of size N

// Geometric matrices of size N × N

