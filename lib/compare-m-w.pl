#!/usr/bin/perl
#
# test if the outputs are the same from MicroJava and WSL

sub read_file($) {
  my ($file) = @_;
  my $in;
  open($in, $file) or die "Cannot read $file: $!\n";
  my $data = join("", <$in>);
  close($in);
  return($data);
}

# quit unless we have the correct number of command-line args
$num_args = $#ARGV + 1;
if ($num_args != 2) {
    print "\nUsage: compare-m-w.pl directory base_filename \n";
    exit;
}

$dir=@ARGV[0];
$base=@ARGV[1];

$outm = read_file("$dir/$base.outmj");
$outw = read_file("$dir/$base.outwsl");

#print "inputs:\n";
#print "$outm\n--\n$outw\n\n";

#process the MJ output to remove comments
$outm=~s/Completion took.*$//s;

#process the WSL output to remove comments
$outw=~s/^.*Starting Execution...//s;
$outw=~s/^.*?\n.*?\n//s;
$outw=~s/Execution time:.*//s;

if ($outm eq $outw) {
		print "$base - OK\n";
} else {
		print "$base - difference detected!:\n";
		print "$outm###\n--\n$outw###\n";
		die();
}