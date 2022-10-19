using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace METOD
{
    public partial class Form1 : Form
    {
        public Form1()
        {
            InitializeComponent();
        }
        void metod1()
        {
            label1.Text = "Shahin Hesenov";
            label2.Text = "17.10.2022";
            label3.Text = "Musfigabad qesebesi";

        }
        void metod2()
        {
            label1.BackColor = Color.Aqua;
            label2.BackColor = Color.BlueViolet;
            label3.BackColor = Color.Aquamarine;
            
        }
        void metod3()
        {
            label1.ForeColor = Color.Brown;
            label2.ForeColor = Color.Coral;
            label3.ForeColor = Color.Beige;
            this.Text = "Ikinci yuzluk";
            MessageBox.Show("Ders Bitdi");
            Application.Exit();
        }
        private void button1_Click(object sender, EventArgs e)
        {
            metod1();
            metod2();
            metod3();
        }
    }
}
