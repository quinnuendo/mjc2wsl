#!/usr/bin/perl
#
# test if the outputs are the same from two versions of
# WSL programs - the original and the transformation

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
    print "\nUsage: compare-w-wt.pl directory base_filename \n";
    exit;
}

$dir=@ARGV[0];
$base=@ARGV[1];

$outw = read_file("$dir/$base.outwsl");
$outwt = read_file("$dir/$base.outwslt");

#print "inputs:\n";
#print "$outm\n--\n$outw\n\n";

#process the WSL output to remove comments
for ($outw, $outwt) {
		s/^.*Starting Execution...//s;
		s/^.*?\n.*?\n//s;
		s/Execution time:.*//s;
}

if ($outw eq $outwt) {
		print "$base - OK\n";
} else {
		print "$base - difference detected!:\n";
		print "$outw###\n--\n$outwt###\n";
		die();
}