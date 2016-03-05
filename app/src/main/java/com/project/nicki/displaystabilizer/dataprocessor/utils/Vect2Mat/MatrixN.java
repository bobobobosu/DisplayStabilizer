package com.project.nicki.displaystabilizer.dataprocessor.utils.Vect2Mat;
public class MatrixN { // N × N matrices
    private VectorN v[];

    MatrixN(int n) {                                    // make a new square matrix
        v = new VectorN[n];
        for (int i = 0 ; i < n ; i++)
            v[i] = new VectorN(n);
    }

    public int size() { return v.length; }                     // return no. of rows

    public double get(int i, int j) { return get(i).get(j); }  // get one element

    public void set(int i, int j, double f) { v[i].set(j,f); } // set one element

    public VectorN get(int i) { return v[i]; }                 // get one row

    public void set(int i, VectorN vec) { v[i].set(vec); }     // set one row

    public void set(MatrixN mat) {                             // copy from another matrix
        for (int i = 0 ; i < size() ; i++)
            set(i, mat.get(i));
    }

    public String toString() {                   // convert to string representation
        String s = "{";
        for (int i = 0 ; i < size() ; i++)
            s += (i == 0 ? "" : ",") + get(i);
        return s + "}";
    }

    public void identity() {                            // set to identity matrix
        for (int j = 0 ; j < size() ; j++)
            for (int i = 0 ; i < size() ; i++)
                set(i, j, (i == j ? 1 : 0));
    }

    public void preMultiply(MatrixN mat) {              // mat × this
        MatrixN tmp = new MatrixN(size());
        double f;

        for (int j = 0 ; j < size() ; j++)
            for (int i = 0 ; i < size() ; i++) {
                f = 0.;
                for (int k = 0 ; k < size() ; k++)
                    f += mat.get(i,k) * get(k,j);
                tmp.set(i, j, f);
            }
        set(tmp);
    }

    public void postMultiply(MatrixN mat) {             // this × mat
        MatrixN tmp = new MatrixN(size());
        double f;

        for (int j = 0 ; j < size() ; j++)
            for (int i = 0 ; i < size() ; i++) {
                f = 0.;
                for (int k = 0 ; k < size() ; k++)
                    f += get(i,k) * mat.get(k,j);
                tmp.set(i, j, f);
            }
        set(tmp);
    }
}
